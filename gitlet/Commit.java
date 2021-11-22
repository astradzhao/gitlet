package gitlet;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/**
 * Represents a gitlet commit object.
 * @author Daniel Zhao
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /**
     * The message of this Commit.
     */
    private String message;
    /**
     * The date of the commit.
     */
    private Date date;
    /**
     * The parent ID of the commit.
     */
    private String parentID;
    /**
     * Hash ID of this commit.
     */
    private String ID;
    /**
     * Blobs in the commit. The key will be name of the file, and the value will be the SHA-1 Hash
     */
    private TreeMap<String, String> blobs;
    /**
     * Branch the commit is in
     */
    private String branch;
    /**
     * Length of initial commit to this commit
     */
    private int length;

    /**
     * Called whenever initializes repo
     */
    public Commit() {
        this.message = "initial commit";
        this.date = new Date(0);
        this.parentID = null;
        this.ID = sha1(serialize(this));
        this.blobs = new TreeMap<>();
        this.branch = "master";
        this.length = 0;
    }

    public Commit(String message, String parentID, TreeMap<String, String> blobs,
                  String branch, int length) {
        this.message = message;
        this.date = new Date();
        this.parentID = parentID;
        this.ID = sha1(serialize(this));
        this.blobs = blobs;
        this.branch = branch;
        this.length = length;
    }

    /**
     * Returns SHA-1 Hash of this commit.
     */
    public String getID() {
        return ID;
    }

    /**
     * Returns parent SHA-1 Hash.
     */
    public String getParentID() {
        return parentID;
    }

    /**
     * Returns blobs within commit.
     */
    public TreeMap<String, String> getBlobs() {
        return blobs;
    }

    /**
     * Returns date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Returns commit message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns formatted date string
     */
    public String getDateString() {
        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        return formatter.format(date);
    }

    /**
     * Adds a file to the blob map, with its file name and hash code
     */
    public void addToBlob(String fileName, String hashCode) {
        this.blobs.put(fileName, hashCode);
    }

    /**
     * Removes file from blob map
     */
    public void removeFromBlob(String fileName) {
        this.blobs.remove(fileName);
    }

    /**
     * Returns whether file is in blob
     */
    public boolean fileinBlob(String fileName) {
        return this.blobs.containsKey(fileName);
    }

    /**
     * Returns the file from a blob based on the file name given
     */
    public File getFile(String fileName) {
        String fileHash = this.blobs.get(fileName);
        if (fileHash == null) {
            return null;
        }
        return join(Repository.BLOBS_PATH, fileHash);
    }

    /**
     * Returns length
     */
    public int getLength() {
        return length;
    }

}
