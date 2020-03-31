package net.vomega.chx;
import java.io.Serializable;

public class ChxSection implements Serializable
{
	private static final long serialVersionUID = 324269464L;
	public boolean supported = false;
	public String name;
	public String label;
    public String nodeid;
    
    public ChxAccount account;
    public ChxCourse course;
    
    public ChxSection() {}
    public ChxSection(ChxCourse course)
    {
    	this.account = course.account;
    	this.course = course;
    }
    
    @Override
	public String toString()
    {
    	if (this.supported)
    		return this.label + " " + this.name;
		else
			return "(Unsupported)";
    }
}