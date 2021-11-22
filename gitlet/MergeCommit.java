package gitlet;

import java.util.TreeMap;

public class MergeCommit extends Commit {
    private String parent2ID;

    public MergeCommit(String message, String parentID, String parent2ID, TreeMap<String,
            String> blobs, String branch, int length) {
        super(message, parentID, blobs, branch, length);
        this.parent2ID = parent2ID;
    }

    public String getParent2ID() {
        return parent2ID;
    }
}
