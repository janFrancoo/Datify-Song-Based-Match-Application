package com.janfranco.datifysongbasedmatchapplication;

public class Issue {

    private String eMail, issue;
    private long createDate;

    Issue(String eMail, String issue, long createDate) {
        this.eMail = eMail;
        this.issue = issue;
        this.createDate = createDate;
    }

    public String geteMail() {
        return eMail;
    }

    public void seteMail(String eMail) {
        this.eMail = eMail;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }
}
