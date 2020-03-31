package net.vomega.chx;
import java.io.Serializable;

public class ChxResource implements Serializable
{
	private static final long serialVersionUID = 424269464L;
	public boolean supported = false;
	public String name;
	public String dtoken;
	public String duration;
	public String downloadurl;
	public String jtoken;
	public String objectid;
	public String jobid;
	
	public ChxAccount account;
	public ChxCourse course;
	public ChxSection section;
	
	public ChxResource() {}
	public ChxResource(ChxSection section)
	{
		this.account = section.account;
		this.course = section.course;
		this.section = section;
	}
	
	@Override
	public String toString()
	{
		if (this.supported)
			return this.name;
		else if (this.name != null)
			return "(仅下载) " + this.name;
		else
			return "(Unsupported)";
	}
}