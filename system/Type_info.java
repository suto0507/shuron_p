package system;

public class Type_info{
	public String type;
	public int dims;
	
	public Type_info(String type, int dims){
		this.type = type;
		this.dims = dims;
	}
	
	public boolean equals(Type_info ti){
		return (this.type.equals(ti.type) && this.dims == ti.dims);
	}
}
