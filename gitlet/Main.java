package gitlet;
import java.io.File;

import static gitlet.Utils.*;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Daniel Zhao
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        boolean hasRepo = Repository.load();
        if(args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if(args[0].equals("init")){
            Repository.init();
            return;
        }
        if (!hasRepo) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "add":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                File newFile = join(Repository.CWD, args[1]);
                if (!newFile.exists()) {
                    System.out.println("File does not exist.");
                    break;
                }
                Repository.addToStage(newFile);
                break;
            case "commit":
                if (args.length < 2) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                if(args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                    break;
                }
                Repository.commitAll(args[1]);
                break;
            case "checkout":
                if (args.length < 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                if (args[1].equals("--")) {
                    if (args.length != 3) {
                        System.out.println("Incorrect operands.");
                        break;
                    }
                    String fileName = args[2];
                    Repository.checkoutFile(fileName);
                }
                if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        break;
                    }
                    String commitID = args[1];
                    if(commitID.length() < 40) {
                        commitID = Repository.abbreviated(commitID);
                        if(commitID == null) {
                            System.out.println("No commit with that id exists.");
                            break;
                        }
                    }
                    String fileName = args[3];
                    Repository.checkoutFile(fileName, commitID);
                }
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                }
                break;
            case "log":
                if (args.length > 1) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.printLog();
                break;
            case "rm":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.removeFile(args[1]);
                break;
            case "global-log":
                if (args.length > 1) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.printGlobalLog();
                break;
            case "find":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.findMessage(args[1]);
                break;
            case "branch":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.createNewBranch(args[1]);
                break;
            case "status":
                if (args.length > 1) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.status();
                break;
            case "rm-branch":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                String commitID = args[1];
                if(commitID.length() < 40) {
                    commitID = Repository.abbreviated(commitID);
                    if(commitID == null) {
                        System.out.println("No commit with that id exists.");
                        break;
                    }
                }
                Repository.reset(commitID);
                break;
            case "merge":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                String branch = args[1];
                if(branch.equals(Repository.getCurrentBranch())) {
                    System.out.println("Cannot merge a branch with itself.");
                    break;
                }
                if(!Repository.getBranches().containsKey(branch)) {
                    System.out.println("A branch with that name does not exist.");
                    break;
                }
                Repository.merge(branch);
                break;
            case "add-remote":
                if (args.length > 3) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.addRemote(args[1], args[2]);
                break;
            case "rm-remote":
                if (args.length > 2) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.rmRemote(args[1]);
                break;
            case "push":
                if (args.length > 3) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.push(args[1], args[2]);
                break;
            case "fetch":
                if (args.length > 3) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.fetch(args[1], args[2]);
                break;
            case "pull":
                if (args.length > 3) {
                    System.out.println("Incorrect operands.");
                    break;
                }
                Repository.pull(args[1], args[2]);
                break;
            default:
                System.out.println("No command with that name exists.");
                break;
        }
    }
}
