package org.lskk.lumen.socmed;

import java.io.Serializable;

/**
 * Created by NADIA on 27/02/2015.
 */
public class UserComment implements Serializable {

    private String postId;
    private String message;

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserComment() {
    }

    public UserComment(String postId, String message) {
        this.postId = postId;
        this.message = message;
    }
}