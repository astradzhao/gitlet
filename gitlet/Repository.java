package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

/**
 * Represents a Gitlet repository, which keeps files and saves commits.
 *
 * @author Daniel Zhao
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    static final File COMMIT_PATH = join(GITLET_DIR, "commits");
    static final File BLOBS_PATH = join(GITLET_DIR, "blobs");
    static final File ADDSTAGE_PATH = join(GITLET_DIR, "addStage");
    static final File RMSTAGE_PATH = join(GITLET_DIR, "rmStage");
    static final File SAVE_DATA = join(GITLET_DIR, "save");
    private static ArrayList<Object> saved = new ArrayList<>();
    private static Commit head;
    private static TreeMap<String, Commit> branches;
    private static String currentBranch;
    private static TreeMap<String, String> remotes;

    /**
     * Initializes gitlet repository
     */
    public static void init() {
        File root = GITLET_DIR;
        if (root.exists()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdir();
        COMMIT_PATH.mkdir();
        BLOBS_PATH.mkdir();
        ADDSTAGE_PATH.mkdir();
        RMSTAGE_PATH.mkdir();
        branches = new TreeMap<>();
        currentBranch = "master";
        remotes = new TreeMap<>();
        Commit initCommit = new Commit();
        saveCommit(initCommit);
        head = initCommit;
        save();
    }

    /**
     * Saves commit in the commits folder with a specific ID
     */
    public static void saveCommit(Commit c) {
        String id = c.getID();
        File commit = join(COMMIT_PATH, id);
        writeObject(commit, c);
        // Updates pointer of branch to be new commit
        branches.put(currentBranch, c);
        save();
    }

    /**
     * Returns the commit with a specific ID
     */
    public static Commit getCommit(String commitID) {
        if (commitID == null) {
            return null;
        }
        File returnCommit = join(COMMIT_PATH, commitID);
        if (!returnCommit.exists()) {
            return null;
        }
        return readObject(returnCommit, Commit.class);
    }

    public static String abbreviated(String abrID) {
        List<String> commitNames = plainFilenamesIn(COMMIT_PATH);
        for (String commitID : commitNames) {
            if (commitID.startsWith(abrID)) {
                return commitID;
            }
        }
        return null;
    }

    /**
     * Persistence after closing
     */
    public static void save() {
        saved.add(0, head);
        saved.add(1, branches);
        saved.add(2, currentBranch);
        saved.add(3, remotes);
        writeObject(SAVE_DATA, saved);
    }

    /**
     * Loads when opening up terminal
     */
    public static boolean load() {
        if (!SAVE_DATA.exists()) {
            return false;
        }
        saved = readObject(SAVE_DATA, ArrayList.class);
        head = (Commit) saved.get(0);
        branches = (TreeMap<String, Commit>) saved.get(1);
        currentBranch = (String) saved.get(2);
        remotes = (TreeMap<String, String>) saved.get(3);
        return true;
    }

    /**
     * Returns head commit from saved data
     */
    public static Commit getHead() {
        return head;
    }

    /**
     * Returns branches map from saved data
     */
    public static TreeMap<String, Commit> getBranches() {
        return branches;
    }

    /**
     * Returns current branch from saved data
     */
    public static String getCurrentBranch() {
        return currentBranch;
    }

    /**
     * Creates file in CWD, using data from blobs folder. Only works if it exists in blob.
     */
    private static void createFileFromBlob(String name, String code) {
        File blob = join(BLOBS_PATH, code);
        if (!blob.exists()) {
            throw new RuntimeException("Wrong use of create file within code.");
        }
        File newFile = join(CWD, name);
        writeContents(newFile, readContents(blob));
    }

    /**
     * Deletes everything in both staging areas.
     */
    public static void clearStage() {
        for (String fName : plainFilenamesIn(ADDSTAGE_PATH)) {
            join(ADDSTAGE_PATH, fName).delete();
        }
        for (String fName : plainFilenamesIn(RMSTAGE_PATH)) {
            join(RMSTAGE_PATH, fName).delete();
        }
    }

    /**
     * Deletes everything in CWD.
     */
    public static void clearCWD() {
        for (String fName : plainFilenamesIn(CWD)) {
            restrictedDelete(join(CWD, fName));
        }
    }

    /**
     * Adds a file to the staging area, and deletes it from the RM staging area.
     */
    public static void addToStage(File f) {
        head = getHead();
        String fileID = sha1(readContents(f));
        String fileName = f.getName();
        // Checks if file is already inside commit
        boolean insideCommit = false;
        String val = head.getBlobs().get(fileName);
        if (val != null && val.equals(fileID)) {
            insideCommit = true;
        }
        if (!insideCommit) {
            File stagedFile = join(ADDSTAGE_PATH, fileName);
            writeContents(stagedFile, readContents(f));
        } else {
            join(ADDSTAGE_PATH, fileName).delete();
        }
        // Deletes if inside delete stage
        join(RMSTAGE_PATH, fileName).delete();
        save();
    }

    /**
     * Commits all files in the staging area into a new commit, and clears the staging area
     */
    public static void commitAll(String message) {
        String parentID = head.getID();
        // Gives names of all files in staging area
        List<String> addedFileList = plainFilenamesIn(ADDSTAGE_PATH);
        List<String> rmedFileList = plainFilenamesIn(RMSTAGE_PATH);
        if (addedFileList.isEmpty() && rmedFileList.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        // Creates new commit
        Commit thisCommit = new Commit(message, parentID, getCommit(parentID).getBlobs(),
                currentBranch, head.getLength() + 1);
        // Adds all files staged for addition
        for (String addFileName : addedFileList) {
            byte[] contents = readContents(join(ADDSTAGE_PATH, addFileName));
            String currentHash = sha1(contents);
            // Adds file to the commit from staging area
            thisCommit.addToBlob(addFileName, currentHash);
            // Creates new file in blobs from staging area
            File committedFile = join(BLOBS_PATH, currentHash);
            writeContents(committedFile, contents);
            // Deletes file in staging area
            join(ADDSTAGE_PATH, addFileName).delete();
        }
        // Removes all files staged for removal
        for (String rmFileName : rmedFileList) {
            thisCommit.removeFromBlob(rmFileName);
            join(RMSTAGE_PATH, rmFileName).delete();
        }
        head = thisCommit;
        saveCommit(thisCommit);
        save();
    }

    /**
     * Checkout command for a file. Checks if file is in the HEAD commit, puts it in the CWD.
     */
    public static void checkoutFile(String fileName) {
        checkoutFile(fileName, head.getID());
    }

    /**
     * Checkout command for a file. Checks if file is in specified commit, puts it in the CWD.
     */
    public static void checkoutFile(String fileName, String commitID) {
        Commit checkedCommit = getCommit(commitID);
        if (checkedCommit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String fileHashCode = checkedCommit.getBlobs().get(fileName);
        if (fileHashCode == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        File checked = join(BLOBS_PATH, fileHashCode);
        writeContents(join(CWD, fileName), readContents(checked));
        save();
    }

    /**
     * Checkout command for branch. Moves CWD and head to head commit of specific branch.
     *
     * @param name Name of the branch to move to.
     */
    public static void checkoutBranch(String name) {
        if (name.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        if (!branches.containsKey(name)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (anyUntracked()) {
            System.out.println("There is an untracked file in the way; delete it, "
                    + "or add and commit it first.");
            return;
        }
        // Deletes everything in CWD
        clearCWD();
        // Sets current head to branch head
        head = branches.get(name);
        // Creates new files in CWD
        TreeMap<String, String> blobs = head.getBlobs();
        for (Map.Entry<String, String> entry : blobs.entrySet()) {
            createFileFromBlob(entry.getKey(), entry.getValue());
        }
        clearStage();
        currentBranch = name;
        save();
    }


    /**
     * Puts file in remove staging area, and removes it from the adding staging area.
     */
    public static void removeFile(String fName) {
        File addedFile = join(ADDSTAGE_PATH, fName);
        // Checks to see if file is in the current commit
        if (!head.fileinBlob(fName) && !addedFile.exists()) {
            System.out.println("No reason to remove the file.");
            return;
        }
        // Deletes file from adding stage
        addedFile.delete();
        if (head.fileinBlob(fName)) {
            // Stages file for removal
            File stagedFile = join(RMSTAGE_PATH, fName);
            File blobFile = head.getFile(fName);
            writeContents(stagedFile, readContents(blobFile));
            // Deletes from working directory
            File f = join(CWD, fName);
            restrictedDelete(f);
        }
        save();
    }

    /**
     * Returns information in a String about current commit
     */
    public static String stringCommit(Commit c) {
        String ts = "===\n";
        ts += "commit " + c.getID() + "\n";
        if (c instanceof MergeCommit) {
            ts += "Merge: " + c.getParentID().substring(0, 7) + " "
                    + ((MergeCommit) c).getParent2ID().substring(0, 7) + "\n";
        }
        ts += "Date: " + c.getDateString() + "\n";
        ts += c.getMessage() + "\n";
        return ts;
    }

    /**
     * Prints log of all commits before current HEAD commit.
     */
    public static void printLog() {
        Commit current = getHead();
        String parentID = current.getParentID();
        System.out.println(stringCommit(current));
        while (parentID != null) {
            current = getCommit(parentID);
            parentID = current.getParentID();
            System.out.println(stringCommit(current));
        }
    }

    /**
     * Prints global log of all commits in repo.
     */
    public static void printGlobalLog() {
        List<String> allCommits = plainFilenamesIn(COMMIT_PATH);
        for (String c : allCommits) {
            System.out.println(stringCommit(getCommit(c)));
        }
    }

    /**
     * Finds and prints all commit IDs with given commit message
     */
    public static void findMessage(String message) {
        List<String> allCommits = plainFilenamesIn(COMMIT_PATH);
        boolean foundOne = false;
        for (String c : allCommits) {
            Commit current = getCommit(c);
            if (current.getMessage().equals(message)) {
                System.out.println(current.getID());
                foundOne = true;
            }
        }
        if (!foundOne) {
            System.out.println("Found no commit with that message.");
            return;
        }
    }

    /**
     * Creates new branch with given name.
     */
    public static void createNewBranch(String name) {
        if (branches.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branches.put(name, head);
        save();
    }

    /**
     * Removes branch with given name.
     */
    public static void removeBranch(String name) {
        if (currentBranch.equals(name)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        if (!branches.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        branches.remove(name);
        save();
    }

    /**
     * Checks out all files in the given commit. Moves current branch's head to that commit node.
     */
    public static void reset(String commitID) {
        if (anyUntracked()) {
            System.out.println("There is an untracked file in the way; delete it, "
                    + "or add and commit it first.");
            return;
        }
        Commit c = getCommit(commitID);
        if (c == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        clearCWD();
        TreeMap<String, String> blobs = c.getBlobs();
        for (String f : blobs.keySet()) {
            checkoutFile(f, commitID);
        }
        clearStage();
        head = c;
        branches.put(currentBranch, c);
        save();
    }

    /**
     * Checks if a file is untracked - it's in CWD but neither staged nor tracked
     *
     * @param fileName name of the file being checked
     * @return true if file is untracked, false if it is tracked
     */
    public static boolean checkUntracked(String fileName) {
        File cwdF = join(CWD, fileName);
        if (!cwdF.exists()) {
            throw new RuntimeException("This should be impossible, wrong use of check in code.");
        }
        File stagedF = join(ADDSTAGE_PATH, fileName);
        // Returns false if it is in either the commit or is staged for addition.
        return !stagedF.exists() && !head.fileinBlob(fileName);
    }

    /**
     * Returns a list of all untracked files in CWD.
     */
    public static ArrayList<String> untrackedInCWD() {
        ArrayList<String> untrackedFiles = new ArrayList<>();
        List<String> filesInCWD = plainFilenamesIn(CWD);
        for (String fName : filesInCWD) {
            if (checkUntracked(fName)) {
                untrackedFiles.add(fName);
            }
        }
        return untrackedFiles;
    }

    /**
     * Returns whether untrackedFiles would also be deleted/modified in a merge
     */
    private static boolean untrackedMerge(Commit current, Commit branch) {
        ArrayList<String> untrackedFiles = new ArrayList<>();
        List<String> filesInCWD = plainFilenamesIn(CWD);
        for (String fName : filesInCWD) {
            if (checkUntracked(fName)) {
                if (!(current.getFile(fName) == null && branch.getFile(fName) == null)) {
                    untrackedFiles.add(fName);
                }
            }
        }
        return untrackedFiles.size() > 0;
    }

    /**
     * Returns whether there are any untracked items in CWD
     */
    public static boolean anyUntracked() {
        return untrackedInCWD().size() > 0;
    }

    /**
     * Returns a map of all files modified, but not staged for commit or committed.
     */
    public static TreeMap<String, String> modifiedNotCommitted() {
        TreeMap<String, String> finalList = new TreeMap<>();
        TreeMap<String, String> blobs = head.getBlobs();
        for (Map.Entry<String, String> entry : blobs.entrySet()) {
            String fileName = entry.getKey();
            String hashVal = entry.getValue();
            File cwdF = join(CWD, fileName);
            // Deleted from CWD, but not staged for removal.
            if (!cwdF.exists()) {
                if (!join(RMSTAGE_PATH, fileName).exists()) {
                    finalList.put(fileName, "deleted");
                }
            } else if (!hashVal.equals(sha1(readContents(cwdF)))) {
                if (!join(ADDSTAGE_PATH, fileName).exists()) {
                    // Modified from CWD and head commit, but not staged.
                    finalList.put(fileName, "modified");
                }
            }
        }
        for (String fileName : plainFilenamesIn(ADDSTAGE_PATH)) {
            String hashStaged = sha1(readContents(join(ADDSTAGE_PATH, fileName)));
            File cwdF = join(CWD, fileName);
            // Staged for addition, but removed from CWD.
            if (!cwdF.exists()) {
                finalList.put(fileName, "deleted");
            } else if (!hashStaged.equals(sha1(readContents(cwdF)))) {
                // Staged for addition, but with different contents than in CWD.
                finalList.put(fileName, "modified");
            }
        }
        return finalList;
    }

    /**
     * Prints out status of the repository.
     */
    public static void status() {
        // Branches
        System.out.println("=== Branches ===");
        List<String> keys = new ArrayList<>(branches.keySet());
//        keys.remove("master");
//        if(currentBranch.equals("master")) {
//            System.out.print("*");
//        }
//        System.out.println("master");
        Collections.sort(keys);
        for (String branch : keys) {
            if (branch.equals(currentBranch)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        // Staged Files
        System.out.println("\n=== Staged Files ===");
        for (String f : plainFilenamesIn(ADDSTAGE_PATH)) {
            System.out.println(f);
        }
        // Removed Files
        System.out.println("\n=== Removed Files ===");
        for (String f : plainFilenamesIn(RMSTAGE_PATH)) {
            System.out.println(f);
        }
        // Modifications
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        TreeMap<String, String> modified = modifiedNotCommitted();
        for (Map.Entry<String, String> entry : modified.entrySet()) {
            String file = entry.getKey();
            String change = entry.getValue();
            System.out.println(file + " (" + change + ")");
        }
        // Untracked Files
        System.out.println("\n=== Untracked Files ===");
        for (String f : untrackedInCWD()) {
            System.out.println(f);
        }
        System.out.println();
    }

    /**
     * Finds and returns the split point of two commits.
     */
    private static String findSplitHelper(Commit c1, Commit c2) {
        ArrayList<String> sC1 = new ArrayList<>();
        ArrayList<String> sC2 = new ArrayList<>();
        HashSet<String> splitsC1 = new HashSet<>(findSplitHelper2(c1, sC1));
        HashSet<String> splitsC2 = new HashSet<>(findSplitHelper2(c2, sC2));
        // List of all common ancestors
        ArrayList<String> common = new ArrayList<>();
        for (String c : splitsC1) {
            if (splitsC2.contains(c)) {
                common.add(c);
            }
        }
        String closest = null;
        int maxVal = -1;
        for (String hash : common) {
            Commit c = getCommit(hash);
            if (c.getLength() > maxVal) {
                closest = hash;
                maxVal = c.getLength();
            }
        }
        return closest;
    }

    /**
     * Helper 2 for findSplit. Finds and returns all ancestors of given commit in an ArrayList.
     */
    private static ArrayList<String> findSplitHelper2(Commit c, ArrayList<String> parents) {
        String id = c.getID();
        parents.add(id);
        if (c.getParentID() == null) {
            return parents;
        }
        parents.addAll(findSplitHelper2(getCommit(c.getParentID()), parents));
        if (c instanceof MergeCommit) {
            parents.addAll(findSplitHelper2(getCommit(((MergeCommit) c).getParent2ID()), parents));
        }
        return parents;
    }

    /**
     * Finds and returns the split point of two branches.
     */
    private static String findSplit(String b1, String b2) {
        Commit b1Head = branches.get(b1);
        Commit b2Head = branches.get(b2);
        return findSplitHelper(b1Head, b2Head);
    }

    /**
     * Replaces contents of conflicting file with fileName with designated strings
     */
    private static void conflictedFile(String fileName, Commit current, Commit branch) {
        File editedFile = join(CWD, fileName);
        File currentFile = current.getFile(fileName);
        File branchFile = branch.getFile(fileName);
        if (currentFile == null) {
            writeContents(editedFile, "<<<<<<< HEAD\n" + "=======\n"
                    + readContentsAsString(branchFile) + ">>>>>>>\n");
        } else if (branchFile == null) {
            writeContents(editedFile, "<<<<<<< HEAD\n"
                    + readContentsAsString(currentFile) + "=======\n" + ">>>>>>>\n");
        } else {
            writeContents(editedFile, "<<<<<<< HEAD\n"
                    + readContentsAsString(currentFile) + "=======\n"
                    + readContentsAsString(branchFile) + ">>>>>>>\n");
        }
        addToStage(editedFile);
        save();
    }

    public static void mergeCommitAll(String message, String branch) {
        String parentID = branches.get(currentBranch).getID();
        String parent2ID = branches.get(branch).getID();
        // Gives names of all files in staging area
        List<String> addedFileList = plainFilenamesIn(ADDSTAGE_PATH);
        List<String> rmedFileList = plainFilenamesIn(RMSTAGE_PATH);
        // Creates new commit
        int length = Math.max(head.getLength(), branches.get(branch).getLength());
        Commit thisCommit = new MergeCommit(message, parentID, parent2ID,
                getCommit(parentID).getBlobs(), currentBranch, length + 1);
        // Adds all files staged for addition
        for (String addFileName : addedFileList) {
            byte[] contents = readContents(join(ADDSTAGE_PATH, addFileName));
            String currentHash = sha1(contents);
            // Adds file to the commit from staging area
            thisCommit.addToBlob(addFileName, currentHash);
            // Creates new file in blobs from staging area
            File committedFile = join(BLOBS_PATH, currentHash);
            writeContents(committedFile, contents);
            // Deletes file in staging area
            join(ADDSTAGE_PATH, addFileName).delete();
        }
        // Removes all files staged for removal
        for (String rmFileName : rmedFileList) {
            thisCommit.removeFromBlob(rmFileName);
            join(RMSTAGE_PATH, rmFileName).delete();
        }
        head = thisCommit;
        saveCommit(thisCommit);
        save();
    }

    /**
     * Returns whether there are any files in staging.
     */

    private static boolean anyInStage() {
        return !plainFilenamesIn(ADDSTAGE_PATH).isEmpty()
                || !plainFilenamesIn(RMSTAGE_PATH).isEmpty();
    }

    /**
     * Merges given branch and current branch together.
     */
    public static void merge(String branch) {
        Commit currentCommit = branches.get(currentBranch);
        if (untrackedMerge(currentCommit, branches.get(branch))) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
        if (anyInStage()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        Commit splitCommit = getCommit(findSplit(branch, currentBranch));
        Commit branchCommit = branches.get(branch);
        String splitID = splitCommit.getID();
        String branchID = branchCommit.getID();
        String currentID = currentCommit.getID();
        boolean conflict = false;
        if (splitID.equals(branchID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        if (splitID.equals(currentID)) {
            checkoutBranch(branch);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        TreeMap<String, String> splitBlobs = splitCommit.getBlobs();
        TreeMap<String, String> currentBlobs = currentCommit.getBlobs();
        TreeMap<String, String> branchBlobs = branchCommit.getBlobs();
        for (Map.Entry<String, String> file : splitBlobs.entrySet()) {
            String fHash = file.getValue();
            String fileName = file.getKey();
            String bHash = branchBlobs.get(fileName);
            String cHash = currentBlobs.get(fileName);
            // Case 1, Case 8
            if (bHash != null && !fHash.equals(bHash)) {
                if (fHash.equals(cHash)) {
                    checkoutFile(fileName, branchID);
                    addToStage(join(CWD, fileName));
                }
                if (cHash != null && !fHash.equals(cHash) && !bHash.equals(cHash)) {
                    conflictedFile(fileName, currentCommit, branchCommit);
                    conflict = true;
                }
            }
            // 8. contents of one are changed and the other file is deleted, replace.
            if (bHash == null && cHash != null && !fHash.equals(cHash)) {
                conflictedFile(fileName, currentCommit, branchCommit);
                conflict = true;
            } else if (cHash == null && bHash != null && !fHash.equals(bHash)) {
                conflictedFile(fileName, currentCommit, branchCommit);
                conflict = true;
            }
            // Case 6
            if (bHash == null && fHash.equals(cHash)) {
                removeFile(fileName);
            }
        }
        // Case 5
        for (Map.Entry<String, String> file : branchBlobs.entrySet()) {
            String fileName = file.getKey();
            String branchHash = file.getValue();
            String splitHash = splitBlobs.get(fileName);
            String cHash = currentBlobs.get(fileName);
            if (cHash == null && splitHash == null) {
                checkoutFile(fileName, branchID);
                addToStage(join(CWD, fileName));
            }
            // 8. File absent at the split point, different contents in branches.
            if (splitHash == null && cHash != null && !cHash.equals(branchHash)) {
                conflictedFile(fileName, currentCommit, branchCommit);
                conflict = true;
            }
        }
        mergeCommitAll("Merged " + branch + " into " + currentBranch + ".", branch);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        save();
    }

    /**
     * Adds a remote to the remote directories list
     */
    public static void addRemote(String name, String dir) {
        if (remotes.keySet().contains(name)) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        dir.replace("/", java.io.File.separator);
        remotes.put(name, dir);
        save();
    }

    /**
     * Remotes a specific remote from the remote directories list
     */
    public static void rmRemote(String name) {
        if (!remotes.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remotes.remove(name);
        save();
    }

    public static ArrayList<Object> remoteLoad(String name) {
        File remoteDir = join(remotes.get(name));
        ArrayList<Object> save = readObject(join(remoteDir, "save"), ArrayList.class);
        return save;
    }

    public static void remoteSave(String name, ArrayList<Object> data) {
        File remoteDir = join(remotes.get(name));
        writeObject(join(remoteDir, "save"), data);
    }

    private static void saveRemoteCommit(Commit c, String name, String branch) {
        ArrayList<Object> save = remoteLoad(name);
        String id = c.getID();
        File commit = join(remotes.get(name), "commits", id);
        writeObject(commit, c);
        // Updates pointer of branch to be new commit
        TreeMap<String, Commit> remoteBranches = (TreeMap<String, Commit>) save.get(1);
        remoteBranches.put(branch, c);
        save.set(1, remoteBranches);
        remoteSave(name, save);
    }

    private static Commit getRemoteCommit(String remoteName, String commitID) {
        File returnCommit = join(remotes.get(remoteName), commitID);
        if (!returnCommit.exists()) {
            return null;
        }
        return readObject(returnCommit, Commit.class);
    }

    /**
     * Pushes current repository commits to remote commit, given that it is in the
     * history of the current commit.
     */
    public static void push(String name, String branch) {
        if (!remotes.containsKey(name)) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteRepo = join(remotes.get(name));
        if (!remoteRepo.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        boolean newBranch = false;
        ArrayList<Object> save = remoteLoad(name);
        TreeMap<String, Commit> remoteBranches = (TreeMap<String, Commit>) save.get(1);
        if (!remoteBranches.containsKey(branch)) {
            remoteBranches.put(branch, head);
            newBranch = true;
        }
        // Remote Branch Head
        Commit rBHead = remoteBranches.get(branch);
        ArrayList<Commit> prevCommits = new ArrayList<>();
        Commit current = head;
        while (!current.getID().equals(rBHead.getID())) {
            prevCommits.add(0, current);
            current = getCommit(current.getParentID());
            if (current == null && !newBranch) {
                System.out.println("Please pull down remote changes before pushing.");
                return;
            }
        }
        for (Commit c : prevCommits) {
            saveRemoteCommit(c, name, branch);
        }
        String remCurrentBranch = (String) save.get(2);
        if (remCurrentBranch.equals(branch)) {
            rBHead = head;
            save = remoteLoad(name);
            save.set(0, rBHead);
            remoteSave(name, save);
        }
    }

    public static void fetch(String name, String branch) {
        if (!remotes.containsKey(name)) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteRepo = join(remotes.get(name));
        if (!remoteRepo.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        ArrayList<Object> save = remoteLoad(name);
        TreeMap<String, Commit> remoteBranches = (TreeMap<String, Commit>) save.get(1);
        if (!remoteBranches.containsKey(branch)) {
            System.out.println("That remote does not have that branch.");
            return;
        }
        File remoteBlobs = join(remotes.get(name), "blobs");
        ArrayList<Commit> newCommits = new ArrayList<>();
        // Remote Branch Head
        Commit current = remoteBranches.get(branch);
        while (current != null) {
            String id = current.getID();
            File commit = join(COMMIT_PATH, id);
            if (!commit.exists()) {
                writeObject(commit, current);
                newCommits.add(0, current);
            }
            current = getRemoteCommit(name, current.getParentID());
        }
        for (Commit c : newCommits) {
            for (Map.Entry<String, String> file : c.getBlobs().entrySet()) {
                String hashCode = file.getValue();
                File newBlob = join(BLOBS_PATH, hashCode);
                writeContents(newBlob, readContents(join(remoteBlobs, hashCode)));
            }
        }
        // Updates head commit of branch
        branches.put(name + "/" + branch, remoteBranches.get(branch));
        save();
    }

    public static void pull(String name, String branch) {
        fetch(name, branch);
        merge(name + "/" + branch);
        save();
    }
}
