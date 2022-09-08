package gitlet;
import java.io.File;


import static gitlet.Utils.*;
/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Therese Pamplona ~
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        // args is empty
        if (args.length < 1) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.init();
                return;
            case "add":
                checkInitExists();
                File x = join(Repository.CWD, args[1]);
                if (!x.exists()) {
                    System.out.println("File does not exist.");
                    System.exit(0);
                }
                Repository.add(args[1]);
                return;
            case "rm":
                checkInitExists();
                File removeRequest = join(StagingArea.ADD_STAGE, args[1]);
                String headFile = Utils.readContentsAsString(Repository.HEAD_POINTER_INDEX);
                File checkHead = join(Repository.COMMIT_STORAGE, headFile);
                Commit parent = Utils.readObject(checkHead, Commit.class);
                if (!removeRequest.exists() && !parent.blobKeys.contains(args[1])) {
                    System.out.println("No reason to remove the file.");
                    System.exit(0);
                } else {
                    Repository.remove(args[1]);
                    return;
                }
            case "branch":
                checkInitExists();
                File checkExists = join(Repository.GITLET_DIR, args[1] + " branch");
                branchHelper(checkExists, args);
                return;
            case "commit":
                checkInitExists();
                commitFailure(args);
                String commitMessage = args[1];
                new Commit(commitMessage);
                return;
            case "checkout": // 3 different kinds of checkout!
                checkInitExists();
                checkoutHelper(args);
                return;
            case "log":
                checkInitExists();
                Commit.log();
                return;
            case "rm-branch":
                checkInitExists();
                StagingArea.rmBranch(args[1]);
                return;
            case "global-log":
                checkInitExists();
                Commit.globalLog();
                return;
            case "find":
                checkInitExists();
                StagingArea.find(args[1]);
                return;
            case "status":
                checkInitExists();
                StagingArea.status();
                return;
            case "reset":
                checkInitExists();
                StagingArea.reset(args[1]);
                return;
            case "merge":
                checkInitExists();
                StagingArea.merge(args[1]);
                return;
        }
        System.out.println("No command with that name exists");
        System.exit(0);
    }
    private static void checkInitExists() {
        File temp = Utils.join(Repository.CWD, ".gitlet");
        if (!temp.exists()) {
            System.out.println("Not an initialized Gitlet directory.");
            System.exit(0);
        }
    }
    private static void commitFailure(String[] args) {
        if (args.length < 2 || args[1].equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
    }
    private static void checkoutHelper(String[] args) {
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands.");
            }
            Repository.checkout(args[2]);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            Repository.checkout(args[1], args[3]);
        } else if (args.length == 2) {
            Repository.checkoutBranch(args[1]);
        }
    }
    private static void branchHelper(File checkExists, String[] args) {
        if (checkExists.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            Commit.newBranch(args[1]);
        }
    }
}
