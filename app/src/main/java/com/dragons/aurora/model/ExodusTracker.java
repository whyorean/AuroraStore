package com.dragons.aurora.model;

public class ExodusTracker {

    public String Name;
    public String URL;
    public String Date;
    public String Description;
    public String Signature;

    public ExodusTracker(String Name, String URL, String Signature, String Date) {
        this.Name = Name;
        this.URL = URL;
        this.Signature = Signature;
        this.Date = Date;
        //this.Description = Description;
    }

}
