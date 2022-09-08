package gitlet;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.*;

/** Represents a gitlet commit object.
 *
 *  does at a high level.
 * @source https://www.javatpoint.com/java-get-current-date
 * @source https://beginnersbook.com/2013/05/current-date-time-in-java/
 * @source https://www.geeksforgeeks.org/how-to-remove-all-white-spaces-from-a-string-in-java/
 * @source https://stackoverflow.com/questions/4917326/how-to-iterate-over-the-files
 *                 -of-a-certain-directory-in-java
 *  @author Mary Therese Pamplona
 */
public class Commit implements Serializable {
    /**
     *
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    protected String message;

    /** The timestamp of this Commit. */
    private String timestamp;

    /** The parent reference of this Commit. */
    private String parentReference;

    /** The SHA-id of this Commit. */
    protected String idSHA;

    /** The blob references of this Commit's files. */
    protected ArrayList<String> blobKeys;

    /** The filename of this Commit. */
    private int myIndex;

    /** The filename of the parent Commit. */
    private Integer parentIndex;
    /** For merge commits*/
    private String secondParent;
    private String secondIndex;

    /** */
    public Commit(String message) {
        this.message = message;
        this.blobKeys = new ArrayList<>();
        if (message.equals("initial commit")) {
            this.timestamp = "Wed Dec 31 16:00:00 1969";
            this.myIndex = 0;
            this.idSHA = returnString();
            submitSHA(this.idSHA);
            updateHeadPointer(this.idSHA, "master");
        } else {
            commitFailureCase();
            if (mergeCommit()) {
                String givenBranch = Utils.readContentsAsString(StagingArea.MERGE_BRANCH);
                File temp = Utils.join(Repository.GITLET_DIR, givenBranch + " branch");
                File tempIndex = Utils.join(Repository.GITLET_DIR, givenBranch + " filename");
                this.secondIndex = Utils.readContentsAsString(tempIndex);
                this.secondParent = Utils.readContentsAsString(temp);
            }
            DateFormat dateFormat = new SimpleDateFormat("E MMM d HH:mm:ss Y");
            Date date = new Date();
            this.timestamp = dateFormat.format(date);
            this.myIndex = getMyIndex();
            this.parentIndex = this.myIndex - 1;
            this.parentReference = getParentSHA();
            this.blobKeys = getParentBlobs();
            computeBlobReferences();
            removeFiles();
            this.idSHA = returnString();
            submitSHA(this.idSHA);
            String headBranch = Utils.readContentsAsString(Repository.HEAD_POINTER_NAME);
            updateHeadPointer(this.idSHA, headBranch);
            updateBranchPaths(headBranch);
        }
    }
    public void commitFailureCase() {
        if (StagingArea.ADD_STAGE.list().length < 1
                && StagingArea.REMOVAL_STAGE.list().length < 1) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
    }
    public static boolean mergeCommit() {
        return Utils.readContentsAsString(StagingArea.MERGE_COMMIT).equals("true");
    }

    public void submitSHA(String q) {
        File temp = Utils.join(Repository.COMMIT_SHA, this.myIndex + " " + q);
        Utils.writeObject(temp, null);
    }

    public int getMyIndex() {
        File[] listFiles = Repository.COMMIT_STORAGE.listFiles();
        return listFiles.length;
    }

    public String returnString() {
        File currFile = Utils.join(Repository.COMMIT_STORAGE, Integer.toString(this.myIndex));
        Utils.writeObject(currFile, this);
        String strFile = Utils.readContentsAsString(currFile);
        return Utils.sha1(strFile);
    }

    /** Calls Blobs.java */
    public void computeBlobReferences() {
        File commitTheseFiles = Utils.join(StagingArea.STAGING_AREA, "addition_stage");
        File[] listFiles = commitTheseFiles.listFiles();
        if (listFiles != null) {
            for (File x : listFiles) {
                byte[] deseVersion = Utils.readContents(x);
                String theSHA = Utils.sha1(deseVersion);
                if (this.blobKeys.contains(x.getName())) {
                    int oldVersion = this.blobKeys.indexOf(x.getName());
                    this.blobKeys.add(oldVersion + 1, this.myIndex + " " + theSHA);
                    this.blobKeys.remove(oldVersion + 2);
                } else {
                    this.blobKeys.add(x.getName());
                    this.blobKeys.add(this.myIndex + " " + theSHA);
                }
                //Adds files to blob storage
                File target = Utils.join(Repository.BLOB_STORAGE, theSHA);
                Utils.writeContents(target, deseVersion);
                x.delete();
            }
        }
    }

    public void removeFiles() {
        File[] removeTheseFiles = StagingArea.REMOVAL_STAGE.listFiles();
        if (removeTheseFiles != null) {
            for (File x : removeTheseFiles) {
                if (this.blobKeys.contains(x.getName())) {
                    int indexOfRemove = this.blobKeys.indexOf(x.getName());
                    this.blobKeys.remove(indexOfRemove);
                    this.blobKeys.remove(indexOfRemove);
                }
                x.delete();
            }
        }
    }

    private void updateHeadPointer(String computedSHA, String currentBranch) {
        File headPath = Utils.join(Repository.GITLET_DIR, currentBranch + " branch");
        File headPathIndex = Utils.join(Repository.GITLET_DIR, currentBranch + " filename");
        Utils.writeContents(Repository.HEAD_POINTER, computedSHA);
        Utils.writeContents(Repository.HEAD_POINTER_INDEX, Integer.toString(this.myIndex));
        Utils.writeContents(headPath, computedSHA);
        Utils.writeContents(headPathIndex, Integer.toString(this.myIndex));
    }

    public static void newBranch(String branchName) {
        File newBranch = Utils.join(Repository.GITLET_DIR, branchName + " branch");
        File newBranchIndex = Utils.join(Repository.GITLET_DIR, branchName + " filename");
        File branchPath = Utils.join(Repository.BRANCH_PATHS, branchName);
        try {
            newBranch.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            newBranchIndex.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            branchPath.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String oldHead = Utils.readContentsAsString(Repository.HEAD_POINTER_NAME);
        File oldHeadBranchPath = Utils.join(Repository.BRANCH_PATHS, oldHead);
        String oldSequence = Utils.readContentsAsString(oldHeadBranchPath);
        Utils.writeContents(branchPath, oldSequence);
        File headPath = Utils.join(Repository.GITLET_DIR, "head pointer filename");
        File headPathSHA = Utils.join(Repository.GITLET_DIR, "head pointer");
        String currHead = Utils.readContentsAsString(headPath);
        String headSHA = Utils.readContentsAsString(headPathSHA);
        Utils.writeContents(newBranchIndex, currHead);
        Utils.writeContents(newBranch, headSHA);
    }

    private Commit getParentReference() {
        String current = Utils.readContentsAsString(Repository.HEAD_POINTER_NAME);
        File path = Utils.join(Repository.BRANCH_PATHS, current);
        String sequence = Utils.readContentsAsString(path);
        List<String> sequenceList = new ArrayList<String>(Arrays.asList(sequence.split(",")));
        File parentFile = Utils.join(Repository.COMMIT_STORAGE,
                sequenceList.get(sequenceList.size() - 1));
        Commit parent = Utils.readObject(parentFile, Commit.class);
        return parent;
    }

    private void updateBranchPaths(String currBranch) {
        File path = Utils.join(Repository.BRANCH_PATHS, currBranch);
        String old = Utils.readContentsAsString(path);
        Utils.writeContents(path, old + "," + myIndex);
        if (mergeCommit()) {
            // Issue with paths!
            Utils.writeContents(path, old + "," + secondIndex + "," + myIndex);
        }
    }

    private String getParentSHA() {
        Commit parent = getParentReference();
        String returnVal1 = parent.returnString();
        return returnVal1;
    }
    private ArrayList<String> getParentBlobs() {
        Commit parent = getParentReference();
        ArrayList<String> returnVal2 = parent.blobKeys;
        return returnVal2;
    }
    public static void log() {
        String curr = Utils.readContentsAsString(Repository.HEAD_POINTER_NAME);
        File path = Utils.join(Repository.BRANCH_PATHS, curr);
        String sequence = Utils.readContentsAsString(path);
        List<String> sequenceList = new ArrayList<String>(Arrays.asList(sequence.split(",")));
        int numCommits = sequenceList.size();
        Collections.reverse(sequenceList);
        for (int i = 0; i < numCommits; i += 1) {
            System.out.println("===");
            File temp = Utils.join(Repository.COMMIT_STORAGE, sequenceList.get(i));
            Commit temp2 = Utils.readObject(temp, Commit.class);
            String commitSHA = temp2.returnString();
            String date = temp2.timestamp;
            String message = temp2.message;
            System.out.println("commit " + commitSHA);
            if (isMerge(temp2)) {
                System.out.println("Merge: " + temp2.parentReference.substring(0, 7) + " "
                        + temp2.secondParent.substring(0, 7));
            }
            System.out.println("Date: " + date + " -0800");
            System.out.println(message);
            System.out.println();
        }
    }
    public static void globalLog() {
        List<String> commitFiles = Utils.plainFilenamesIn(Repository.COMMIT_STORAGE);
        Collections.reverse(commitFiles);
        for (String x : commitFiles) {
            FilenameFilter correlatedSHA = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(x);
                }
            };
            File path = Utils.join(Repository.COMMIT_STORAGE, x);
            String[] pathSHA = Repository.COMMIT_SHA.list(correlatedSHA);
            Commit curr = Utils.readObject(path, Commit.class);
            System.out.println("===");
            System.out.println("commit " + pathSHA[0].substring(2));
            if (isMerge(curr)) {
                System.out.println("Merge " + curr.parentReference.substring(0, 8) + " "
                        + curr.secondParent.substring(0, 8));
            }
            System.out.println("Date: " + curr.timestamp + " -0800");
            System.out.println(curr.message);
            System.out.println();
        }
    }

    public static boolean isMerge(Commit commit) {
        return commit.secondParent != null;

    }

}
