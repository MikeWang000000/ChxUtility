package net.vomega.chx;
import java.io.Serializable;

public class ChxAccount implements Serializable
{
	private static final long serialVersionUID = 124269464L;
    public String username;
    public String password;
    public String userid;
    public String realname;
    public String schoolid;
    
    @Override
	public String toString()
    {
    	return this.realname + "[" + this.userid + "] (" + this.username + ")";
    }
}