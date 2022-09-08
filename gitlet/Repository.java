package gitlet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/** Represents a gitlet repository.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *  @source https://www.geeksforgeeks.org/sha-1-hash-in-java/
 * @source https://stackabuse.com/java-list-files-in-a-directory/
 * @source https://stackoverflow.com/questions/5943330/common-elements-in-two-lists
 * @source https://stackoverflow.com/questions/13286008/find-out-the-elements-of-an-arraylist-which-
 *          is-not-present-in-another-arraylist/13286094
 *  @author Mary Therese Pamplona
 */
public class Repository implements Serializable {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");
    /** A file that contains the SHA-id of the master branch. */
    public static final File MASTER_BRANCH = Utils.join(GITLET_DIR, "master branch");
    /** A file that contains the index/filename of the MASTER_BRANCH. */
    public static final File MASTER_BRANCH_INDEX = Utils.join(GITLET_DIR, "master filename");
    /** A file that contains the SHA-id of the head pointer. */
    public static final File HEAD_POINTER = Utils.join(GITLET_DIR, "head pointer");
    /** A file that contains the index/filename of the HEAD_POINTER. */
    public static final File HEAD_POINTER_INDEX = Utils.join(GITLET_DIR, "head pointer filename");
    /** A file that contains the name of the current branch. */
    public static final File HEAD_POINTER_NAME = Utils.join(GITLET_DIR, "head pointer branch name");
    /** A directory that stores Blobs and Commits. */
    public static final File MEMORY = Utils.join(GITLET_DIR, "objects");
    /** A directory that stores Blobs. */
    public static final File BLOB_STORAGE = Utils.join(MEMORY, "blob_storage");
    /** A directory that stores Commits. */
    public static final File COMMIT_STORAGE = Utils.join(MEMORY, "commit_storage");
    /** A directory in which the filenames are (commit num) shaID (empty file) */
    public static final File COMMIT_SHA = Utils.join(MEMORY, "commit_shas");
    public static final File BRANCH_PATHS = Utils.join(MEMORY, "branch_paths");

    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already"
                    + "exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        MEMORY.mkdir();
        BLOB_STORAGE.mkdir();
        COMMIT_STORAGE.mkdir();
        COMMIT_SHA.mkdir();
        BRANCH_PATHS.mkdir();
        if (!MASTER_BRANCH.exists()) {
            try {
                MASTER_BRANCH.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!HEAD_POINTER.exists()) {
            try {
                HEAD_POINTER.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!MASTER_BRANCH_INDEX.exists()) {
            try {
                MASTER_BRANCH_INDEX.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!HEAD_POINTER_INDEX.exists()) {
            try {
                HEAD_POINTER_INDEX.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            HEAD_POINTER_NAME.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(HEAD_POINTER_NAME, "master");
        StagingArea.setupStagingArea();
        new Commit("initial commit");
    }

    /** Helper method that computes the SHA-id of a given file. */
    public static String computeSHA(File f) {
        byte[] conversion = Utils.readContents(f);
        String x = Utils.sha1(conversion);
        return x;
    }

    /** */
    public static void add(String filename) {
        File submittedVersion = Utils.join(Repository.CWD, filename);
        String newAdditionSHA = computeSHA(submittedVersion);
        // CASE WHERE THE CURRENT WORKING VERSION IDENTICAL TO CURRENT COMMIT
        String commitFileSHAval = commitFileSHA(filename);
        if (commitFileSHAval != null) {
            String currCommitSHA = commitFileSHAval.substring(2);
            if (newAdditionSHA.equals(currCommitSHA)) {
                File removeFromRStage = Utils.join(StagingArea.REMOVAL_STAGE, filename);
                File removeFromAStage = Utils.join(StagingArea.ADD_STAGE, filename);
                removeFromRStage.delete();
                removeFromAStage.delete();
                System.exit(0);
            }
        }
        // CASE WHERE FILE IS ALREADY IN STAGING AREA -- OVERWRITE
        File target = Utils.join(StagingArea.ADD_STAGE, filename);
        byte[] deseVersion = Utils.readContents(submittedVersion);
        Utils.writeContents(target, deseVersion);
    }

    public static void remove(String filename) {
        File cwdFile = Utils.join(Repository.CWD, filename);
        File checkStage = Utils.join(StagingArea.ADD_STAGE, filename);
        if (checkStage.exists()) {
            checkStage.delete();
        }
        String headIndex = Utils.readContentsAsString(Repository.HEAD_POINTER_INDEX);
        File headFile = Utils.join(Repository.COMMIT_STORAGE, headIndex);
        Commit headCommit = Utils.readObject(headFile, Commit.class);
        if (headCommit.blobKeys.contains(filename)) {
            File stageForRemoval = Utils.join(StagingArea.REMOVAL_STAGE, filename);
            byte[] deseVersion = removeHelper(headCommit, filename);
            Utils.writeContents(stageForRemoval, deseVersion);
        }
        if (cwdFile.exists() && headCommit.blobKeys.contains(filename)) {
            cwdFile.delete();
        }
    }
    public static byte[] removeHelper(Commit curr, String filename) {
        int index = curr.blobKeys.indexOf(filename);
        String blobSHA = curr.blobKeys.get(index + 1).substring(2);
        File blobPath = Utils.join(Repository.BLOB_STORAGE, blobSHA);
        byte[] deseVersion = Utils.readContents(blobPath);
        return deseVersion;
    }


    /** Returns SHA value of the current Commit's record of a file. */
    public static String commitFileSHA(String filename) {
        String latestCommit = Utils.readContentsAsString(Repository.MASTER_BRANCH_INDEX);
        File temp = Utils.join(Repository.COMMIT_STORAGE, latestCommit);
        Commit curr = Utils.readObject(temp, Commit.class);
        ArrayList<String> blobx = curr.blobKeys;
        if (blobx.contains(filename)) {
            return blobx.get(blobx.indexOf(filename) + 1);
        } else {
            return null;
        }
    }

    public static String checkoutGetSHA(String filename) {
        String commitFileName = Utils.readContentsAsString(Repository.HEAD_POINTER_INDEX);
        File temp = Utils.join(Repository.COMMIT_STORAGE, commitFileName);
        Commit curr = Utils.readObject(temp, Commit.class);
        ArrayList<String> thisCommitBlobs = curr.blobKeys;
        if (thisCommitBlobs.contains(filename)) {
            return thisCommitBlobs.get(thisCommitBlobs.indexOf(filename) + 1);
        }
        System.out.println("File does not exist in that commit.");
        System.exit(0);
        return "Error in checkoutGetSHA method";
    }

    public static void checkout(String filename) {
        File cwd = Utils.join(CWD, filename);
        String fileSHA = checkoutGetSHA(filename).substring(2);
        File checkoutFile = Utils.join(Repository.BLOB_STORAGE, fileSHA);
        String cwdVersionSHA = Utils.sha1(Utils.readContents(cwd));
        if (fileSHA != null && !cwdVersionSHA.equals(fileSHA)) {
            byte[] deseVersion = Utils.readContents(checkoutFile);
            Utils.writeContents(cwd, deseVersion);
        } else {
            System.exit(0);
        }
    }
    public static void checkout(String commitID, String filename) {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(commitID);
            }
        };
        String[] temp;
        temp = Repository.COMMIT_SHA.list(filter);
        if (temp.length < 1) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        String fileIndex = temp[0].substring(0, 1);
        File tempPath = Utils.join(Repository.COMMIT_STORAGE, fileIndex);
        Commit search = Utils.readObject(tempPath, Commit.class);
        if (search.blobKeys.contains(filename)) {
            String fileKey = search.blobKeys.get(search.blobKeys.indexOf(filename) + 1);
            File fileSHA = Utils.join(Repository.BLOB_STORAGE, fileKey.substring(2));
            File target = Utils.join(Repository.CWD, filename);
            byte[] deseVersion = Utils.readContents(fileSHA);
            Utils.writeContents(target, deseVersion);
        } else {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }
    public static void checkoutBranch(String branchName) {
        String currentHead = Utils.readContentsAsString(Repository.HEAD_POINTER_NAME);
        if (checkUntracked(branchName)) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }
        if (currentHead.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        ArrayList<String> checkedOutBranch = new ArrayList<>();
        File givenBranch = Utils.join(Repository.GITLET_DIR, branchName + " filename");
        String headIndex = Utils.readContentsAsString(givenBranch);
        File headPath = Utils.join(Repository.COMMIT_STORAGE, headIndex);
        Commit headCommit = Utils.readObject(headPath, Commit.class);
        for (int i = 0; i < headCommit.blobKeys.size(); i += 2) {
            File filePath = Utils.join(Repository.CWD, headCommit.blobKeys.get(i));
            File blobStorage = Utils.join(Repository.BLOB_STORAGE,
                    headCommit.blobKeys.get(i + 1).substring(2));
            Utils.writeContents(filePath, Utils.readContents(blobStorage));
            checkedOutBranch.add(headCommit.blobKeys.get(i));
        }
        String currHeadIndex = Utils.readContentsAsString(Repository.HEAD_POINTER_INDEX);
        File currHeadPath = Utils.join(Repository.COMMIT_STORAGE, currHeadIndex);
        Commit currHeadCommit = Utils.readObject(currHeadPath, Commit.class);
        ArrayList<String> tempCurrentBranch = new ArrayList<>();
        for (int i = 0; i < currHeadCommit.blobKeys.size(); i += 2) {
            tempCurrentBranch.add(currHeadCommit.blobKeys.get(i));
        }
        tempCurrentBranch.removeAll(checkedOutBranch);
        if (tempCurrentBranch.size() > 0) {
            for (int i = 0; i < tempCurrentBranch.size(); i++) {
                File path = Utils.join(Repository.CWD, tempCurrentBranch.get(i));
                path.delete();
            }
        }
        for (File f: Objects.requireNonNull(StagingArea.ADD_STAGE.listFiles())) {
            f.delete();
        }
        for (File f: Objects.requireNonNull(StagingArea.REMOVAL_STAGE.listFiles())) {
            f.delete();
        }
        File x = Utils.join(Repository.GITLET_DIR, branchName + " filename");
        String updateIndex = Utils.readContentsAsString(x);

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(updateIndex);
            }
        };
        String[] temp;
        temp = Repository.COMMIT_SHA.list(filter);

        Utils.writeContents(Repository.HEAD_POINTER_NAME, branchName);
        Utils.writeContents(Repository.HEAD_POINTER_INDEX, updateIndex);
        Utils.writeContents(Repository.HEAD_POINTER, temp[0].substring(2));

    }
    public static boolean checkUntracked(String otherBranch) {
        File tempOther = Utils.join(Repository.GITLET_DIR, otherBranch + " filename");
        if (!tempOther.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String otherIndex = Utils.readContentsAsString(tempOther);
        File otherPath = Utils.join(Repository.COMMIT_STORAGE, otherIndex);
        Commit otherCommit = Utils.readObject(otherPath, Commit.class);
        String[] cwdFiles = CWD.list();
        List<String> common = new ArrayList<String>(otherCommit.blobKeys);
        common.retainAll(Arrays.asList(cwdFiles));
        if (common.size() > 0) {
            String currentBranchIndex = Utils.readContentsAsString(Repository.HEAD_POINTER_INDEX);
            File headPath = Utils.join(Repository.COMMIT_STORAGE, currentBranchIndex);
            Commit headCommit = Utils.readObject(headPath, Commit.class);
            List<String> checkHead = new ArrayList<String>(headCommit.blobKeys);
            common.removeIf(i -> checkHead.contains(i));
            if (common.size() > 0) {
                return true;
            }
        }
        return false;
    }
    public static boolean checkUntrackedReset(String commitID, String index) {
        File resetPath = Utils.join(Repository.COMMIT_STORAGE, index);
        Commit resetCommit = Utils.readObject(resetPath, Commit.class);
        String[] cwdFiles = CWD.list();
        List<String> common = new ArrayList<String>(resetCommit.blobKeys);
        common.retainAll(Arrays.asList(cwdFiles));
        if (common.size() > 0) {
            String currentBranchIndex = Utils.readContentsAsString(Repository.HEAD_POINTER_INDEX);
            File headPath = Utils.join(Repository.COMMIT_STORAGE, currentBranchIndex);
            Commit headCommit = Utils.readObject(headPath, Commit.class);
            List<String> checkHead = new ArrayList<String>(headCommit.blobKeys);
            common.removeIf(i -> checkHead.contains(i));
            if (common.size() > 0) {
                return true;
            }
        }
        return false;
    }
}
