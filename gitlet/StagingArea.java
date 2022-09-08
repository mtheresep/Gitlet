package gitlet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static gitlet.Repository.*;


public class StagingArea implements Serializable {
    /** The staging area directory */
    public static final File STAGING_AREA = Utils.join(GITLET_DIR, "STAGING_AREA");
    public static final File REMOVAL_STAGE = Utils.join(STAGING_AREA, "removal_stage");
    public static final File ADD_STAGE = Utils.join(STAGING_AREA, "addition_stage");
    public static final File MERGE_COMMIT = Utils.join(STAGING_AREA, "merge_commit");
    public static final File MERGE_BRANCH = Utils.join(STAGING_AREA, "merge_branch");

    /**
     * */
    public static void setupStagingArea() {
        if (!STAGING_AREA.exists()) {
            STAGING_AREA.mkdir();
        }
        if (!REMOVAL_STAGE.exists()) {
            REMOVAL_STAGE.mkdir();
        }
        if (!ADD_STAGE.exists()) {
            ADD_STAGE.mkdir();
        }
        File masterPath = Utils.join(BRANCH_PATHS, "master");
        try {
            masterPath.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(masterPath, "0");
        try {
            MERGE_COMMIT.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(MERGE_COMMIT, "false");
        try {
            MERGE_BRANCH.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void rmBranch(String branchName) {
        String currBranch = Utils.readContentsAsString(Repository.HEAD_POINTER_NAME);
        if (currBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File branch = Utils.join(GITLET_DIR, branchName + " branch");
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        branch.delete();
        File branchIndex = Utils.join(GITLET_DIR, branchName + " filename");
        branchIndex.delete();
    }

    public static void find(String commitMessage) {
        int count = 0;
        List<String> files = Utils.plainFilenamesIn(Repository.COMMIT_STORAGE);
        for (String f : Objects.requireNonNull(files)) {
            File tempPath = Utils.join(Repository.COMMIT_STORAGE, f);
            Commit curr = Utils.readObject(tempPath, Commit.class);
            String commitSHA = curr.returnString();
            if (curr.message.equals(commitMessage)) {
                System.out.println(commitSHA);
                count++;
            }
        }
        if (count < 1) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        //BRANCH
        FilenameFilter branch = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("branch");
            }
        };
        String[] branches = GITLET_DIR.list(branch);
        if (branches != null) {
            Collections.sort(Arrays.asList(branches));
        }
        String headBranch = Utils.readContentsAsString(Repository.HEAD_POINTER_NAME);
        System.out.println("=== Branches ===");
        for (String b : Objects.requireNonNull(branches)) {
            int delete = b.indexOf(" ");
            if (b.substring(0, delete).equals(headBranch)) {
                System.out.println("*" + b.substring(0, delete));
                continue;
            }
            System.out.println(b.substring(0, delete));
        }
        System.out.println();
        // Staged Files
        List<String> stagedFiles = Utils.plainFilenamesIn(ADD_STAGE);
        System.out.println("=== Staged Files ===");
        if (stagedFiles != null) {
            for (String file : stagedFiles) {
                System.out.println(file);
            }
        }
        System.out.println();
        // Removed Files
        List<String> removedFiles = Utils.plainFilenamesIn(REMOVAL_STAGE);
        System.out.println("=== Removed Files ===");
        if (removedFiles != null) {
            for (String rFile : removedFiles) {
                System.out.println(rFile);
            }
        }
        System.out.println();
        // Modifications
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }
    public static void reset(String commitID) {
        FilenameFilter sha = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(commitID);
            }
        };
        String[] index = Repository.COMMIT_SHA.list(sha);
        if (index == null || index.length < 1) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        String cleanIndex = index[0].substring(0, 1);
        //change made
        if (Repository.checkUntrackedReset(commitID, cleanIndex)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }
        File path = Utils.join(Repository.COMMIT_STORAGE, cleanIndex);
        Commit curr = Utils.readObject(path, Commit.class);
        String[] cwdFiles = CWD.list();
        List<String> clean = new ArrayList<String>(Arrays.asList(cwdFiles));
        clean.remove("gitlet");
        clean.remove("pom.xml");
        clean.remove("Makefile");
        clean.remove("gitlet-design.md");
        clean.remove("testing");
        clean.remove(".idea");
        clean.remove(".gitlet");
        ArrayList<String> filesInThisCommit = new ArrayList<>();
        for (int i = 0; i < curr.blobKeys.size(); i += 2) {
            checkout(commitID, curr.blobKeys.get(i));
            filesInThisCommit.add(curr.blobKeys.get(i));
        }
        clean.removeAll(filesInThisCommit);
        if (clean.size() > 0) {
            for (String x : clean) {
                File tempPath = Utils.join(CWD, x);
                tempPath.delete();
            }
        }
        for (File f: Objects.requireNonNull(StagingArea.ADD_STAGE.listFiles())) {
            f.delete();
        }
        for (File f: Objects.requireNonNull(StagingArea.REMOVAL_STAGE.listFiles())) {
            f.delete();
        }
        Utils.writeContents(HEAD_POINTER, commitID);
        Utils.writeContents(HEAD_POINTER_INDEX, cleanIndex);
        String currHead = Utils.readContentsAsString(HEAD_POINTER_NAME);
        File headBranch = Utils.join(GITLET_DIR, currHead + " branch");
        Utils.writeContents(headBranch, commitID);
        File headFile = Utils.join(GITLET_DIR, currHead + " filename");
        Utils.writeContents(headFile, cleanIndex);
        //fix the head's branch_paths
        saveOldPath(currHead);
        File x = Utils.join(BRANCH_PATHS, currHead);
        String currentBranchPath = Utils.readContentsAsString(x);
        List<String> sequenceList;
        sequenceList = new ArrayList<String>(Arrays.asList(currentBranchPath.split(",")));
        if (sequenceList.contains(cleanIndex)) {
            fix(sequenceList, cleanIndex);
        } else {
            fix2(sequenceList, cleanIndex);
        }
    }
    public static void fix(List<String> path, String index) {
        Collections.sort(path);
        path.removeIf(i -> Integer.parseInt(i) > Integer.parseInt(index));
        String commaSep = String.join(",", path);
        String headName = Utils.readContentsAsString(HEAD_POINTER_NAME);
        File temp = Utils.join(BRANCH_PATHS, headName);
        Utils.writeContents(temp, commaSep);
    }
    public static void fix2(List<String> path, String index) {
        Collections.sort(path);
        path.removeIf(i -> Integer.parseInt(i) > Integer.parseInt(index));
        for (String x : Objects.requireNonNull(Utils.plainFilenamesIn(BRANCH_PATHS))) {
            File temp = Utils.join(BRANCH_PATHS, x);
            String read = Utils.readContentsAsString(temp);
            List<String> sequence = new ArrayList<>(Arrays.asList(read.split(",")));
            if (sequence.contains(index)) {
                sequence.removeIf(i -> Integer.parseInt(i) > Integer.parseInt(index));
                String commaSep = String.join(",", sequence);
                String headName = Utils.readContentsAsString(HEAD_POINTER_NAME);
                File temp3 = Utils.join(BRANCH_PATHS, headName);
                Utils.writeContents(temp3, commaSep);
            } else {
                continue;
            }
        }
    }
    public static void saveOldPath(String head) {
        File temp = Utils.join(BRANCH_PATHS, head);
        String oldContents = Utils.readContentsAsString(temp);
        int num = getIndex();
        File newFile = Utils.join(BRANCH_PATHS, Integer.toString(num) + "-old-" + head);
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(newFile, oldContents);
    }
    public static int getIndex() {
        File[] listFiles = BRANCH_PATHS.listFiles();
        return listFiles.length;
    }
    public static void merge(String givenBranch) {
        mergeFailureCases(givenBranch);
        Utils.writeContents(MERGE_BRANCH, givenBranch);
        boolean conflict = false;
        // Find split point index
        String splitPoint = findLatestAncestor(givenBranch);
        mergeFailure(givenBranch, splitPoint);
        // Get list of files between split, given, and head
        File splitCommitPath = Utils.join(COMMIT_STORAGE, splitPoint);
        Commit splitCommit = Utils.readObject(splitCommitPath, Commit.class);
        File givenBranchIndexPath = Utils.join(GITLET_DIR, givenBranch + " filename");
        String givenBranchCommitIndex = Utils.readContentsAsString(givenBranchIndexPath);
        File givenCommitPath = Utils.join(COMMIT_STORAGE, givenBranchCommitIndex);
        Commit givenCommit = Utils.readObject(givenCommitPath, Commit.class);
        File headPath = Utils.join(COMMIT_STORAGE, Utils.readContentsAsString(HEAD_POINTER_INDEX));
        Commit headCommit = Utils.readObject(headPath, Commit.class);
        List<String> allFiles = getFileList(splitCommit, givenCommit, headCommit);
        String givenSHA = givenCommit.returnString();
        // Compare the different versions of files between these commits
        for (int i = 0; i < allFiles.size(); i++) {
            // Getting the SHA-1 iD's of the same file in different Commits.
            int splitIndex = splitCommit.blobKeys.indexOf(allFiles.get(i)) + 1;
            int headIndex = headCommit.blobKeys.indexOf(allFiles.get(i)) + 1;
            int givenIndex = givenCommit.blobKeys.indexOf(allFiles.get(i)) + 1;
            String splitVersion = mergeHelper(splitIndex, splitCommit);
            String headVersion = mergeHelper(headIndex, headCommit);
            String givenVersion = mergeHelper(givenIndex, givenCommit);
            // Rule 4 -- only present in head --> stay as is
            if (splitVersion.equals("empty") && givenVersion.equals("empty")
                    && !headVersion.equals("empty")) {
                continue;
            }
            // Rule 5 -- only present in given --> checkout given version file and stage
            if (splitVersion.equals("empty") && headVersion.equals("empty")
                    && !givenVersion.equals("empty")) {
                checkout(givenSHA, allFiles.get(i));
                stage(allFiles.get(i), givenVersion, ADD_STAGE);
                continue;
            }
            // Rule 6 -- split == head, given == empty --> remove
            if (splitVersion.equals(headVersion) && givenVersion.equals("empty")) {
                File temp = Utils.join(Repository.CWD, allFiles.get(i));
                temp.delete();
                continue;
            }
            // Rule 7 -- split == given, head == empty --> stay as is
            if (splitVersion.equals(givenVersion) && headVersion.equals("empty")) {
                continue;
            }
            // Rule 1 -- split != given, split == head --> given
            if (!splitVersion.equals(givenVersion) && splitVersion.equals(headVersion)) {
                checkout(givenSHA, allFiles.get(i));
                stage(allFiles.get(i), givenVersion, ADD_STAGE);
                continue;
            }
            // Rule 2 -- split == given, split != head --> stay as is
            if (splitVersion.equals(givenVersion) && !splitVersion.equals(headVersion)) {
                continue;
            }
            // Rule 3 -- split != given, split != head
            if (!splitVersion.equals(givenVersion) && !splitVersion.equals(headVersion)) {
                if (givenVersion.equals(headVersion)) {
                    continue;
                } else {
                    conflict(allFiles.get(i), headVersion, givenVersion);
                    conflict = true;
                }
            }

        }
        Utils.writeContents(MERGE_COMMIT, "true");
        Utils.writeContents(MERGE_BRANCH, givenBranch);
        new Commit("Merged " + givenBranch + " into "
                + Utils.readContentsAsString(HEAD_POINTER_NAME) + ".");
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        Utils.writeContents(MERGE_BRANCH, "");
        Utils.writeContents(MERGE_COMMIT, "false");
    }
    private static void conflict(String filename, String head, String given) {
        String headContents;
        String givenContents;
        if (head.equals("empty")) {
            headContents = "";
        } else {
            File headPath = Utils.join(BLOB_STORAGE, head);
            headContents = Utils.readContentsAsString(headPath);
        }
        if (given.equals("empty")) {
            givenContents = "";
        } else {
            File givenPath = Utils.join(BLOB_STORAGE, given);
            givenContents = Utils.readContentsAsString(givenPath);
        }
        changeCWD(filename, "<<<<<<< HEAD\n" + headContents
                + "=======\n" + givenContents + ">>>>>>>\n");
    }
    private static void stage(String filename, String fileblob, File stage) {
        File temp = Utils.join(BLOB_STORAGE, fileblob);
        String contents = Utils.readContentsAsString(temp);
        File create = Utils.join(stage, filename);
        try {
            create.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(create, contents);
    }
    private static void changeCWD(String filename, String updatedContents) {
        File cwdPath = Utils.join(Repository.CWD, filename);
        Utils.writeContents(cwdPath, updatedContents);
        Repository.add(filename);
    }

    private static void mergeFailureCases(String givenBranch) {
        if (Objects.requireNonNull(ADD_STAGE.list()).length > 0
                || Objects.requireNonNull(REMOVAL_STAGE.list()).length > 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        File checkExistence = Utils.join(GITLET_DIR, givenBranch + " branch");
        if (!checkExistence.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String currentBranch = Utils.readContentsAsString(HEAD_POINTER_NAME);
        if (currentBranch.equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (Repository.checkUntracked(givenBranch)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }
    }
    private static String findLatestAncestor(String givenBranch) {
        String currentBranch = Utils.readContentsAsString(HEAD_POINTER_NAME);
        File currentPath = Utils.join(BRANCH_PATHS, currentBranch);
        String currentContents = Utils.readContentsAsString(currentPath);
        List<String> currentContentsList;
        currentContentsList = new ArrayList<String>(Arrays.asList(currentContents.split(",")));
        File givenPath = Utils.join(BRANCH_PATHS, givenBranch);
        String givenContents = Utils.readContentsAsString(givenPath);
        List<String> givenContentsList;
        givenContentsList = new ArrayList<String>(Arrays.asList(givenContents.split(",")));
        List<String> common = new ArrayList<String>(givenContentsList);
        common.retainAll(currentContentsList);
        Collections.sort(common);
        return common.get(common.size() - 1);
    }
    private static List<String> getFileList(Commit split, Commit given, Commit head) {
        List<String> allFiles = new ArrayList<>();
        for (int i = 0; i < given.blobKeys.size(); i += 2) {
            allFiles.add(given.blobKeys.get(i));
        }
        for (int i = 0; i < split.blobKeys.size(); i += 2) {
            if (!allFiles.contains(split.blobKeys.get(i))) {
                allFiles.add(split.blobKeys.get(i));
            }
        }
        for (int i = 0; i < head.blobKeys.size(); i += 2) {
            if (!allFiles.contains(head.blobKeys.get(i))) {
                allFiles.add(head.blobKeys.get(i));
            }
        }
        return allFiles;
    }
    /** Checks if split point is the same commit as the given branch and
     *  if the split point is the current branch.
     *  givenBranch is the branch name
     *  splitPoint is the index of the Commit of the split point.
     *  */
    private static void mergeFailure(String givenBranch, String splitPoint) {
        File givenIndexFile = Utils.join(GITLET_DIR, givenBranch + " filename");
        String givenIndex = Utils.readContentsAsString(givenIndexFile);
        if (givenIndex.equals(splitPoint)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        String headIndex = Utils.readContentsAsString(HEAD_POINTER_INDEX);
        if (headIndex.equals(splitPoint)) {
            Repository.checkoutBranch(givenBranch);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    /** Returns null if the Commit does not contain the file else it returns
     * the SHA-1 of the file. */
    private static String mergeHelper(int index, Commit version) {
        if (index < 1) {
            return "empty";
        } else {
            return version.blobKeys.get(index).substring(2);
        }
    }
}
