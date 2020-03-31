package net.vomega.chx;
import java.io.Serializable;

public class ChxCourse implements Serializable
{
	private static final long serialVersionUID = 224269464L;
	public boolean supported = false;
	public String name;
	public String urlcpi;
	public String clazzid;
	public String courseid;
	public String teacher;
	
	public ChxAccount account;
	
	public ChxCourse() {}
	public ChxCourse(ChxAccount account)
	{
		this.account = account;
	}
	
	@Override
	public String toString()
	{
		if (this.supported)
			return this.name + " - " + this.teacher;
		else
			return "(Unsupported)";
	}
}
