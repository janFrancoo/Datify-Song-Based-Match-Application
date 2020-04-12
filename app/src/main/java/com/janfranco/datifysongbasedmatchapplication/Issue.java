package com.janfranco.datifysongbasedmatchapplication;

public class Issue {

    private String eMail, issue;

    // ToDo: Add create time

    Issue(String eMail, String issue) {
        this.eMail = eMail;
        this.issue = issue;
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

}
