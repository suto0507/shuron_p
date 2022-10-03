package system;

public class Source {
	private String s;
	private int position,line,count;
	private int deepestline,deepestcount;
	public int parsed_length;
	
	public Source(String s){
		this.s = s;
		this.line = 1;
		this.count = 1;
		this.deepestcount = 1;
		this.deepestline = 1;
		
	}
	
	public char positionChar() throws Exception{
		if (this.position >= s.length()){
			throw new Exception("end of source");
		}
		return s.charAt(this.position);
	}
	
	public void next() throws Exception {
		if(positionChar()=='\n'){
			if(this.deepestcount<=this.count && this.deepestline==this.line || this.deepestline<this.line){
				this.deepestline++;
				this.deepestcount = 1;
			}
			line++;
			count = 1;
			
		}else{
			if(this.deepestcount<=this.count && this.deepestline==this.line || this.deepestline<this.line) this.deepestcount++;
			this.count++;
		}
		
		
		
		this.position++;
	}
	
	public void where(){
		System.out.println("line is " + this.line + ",count is " + this.count);
	}
	
	public void deepestwhere(){
		System.out.println("deepestline is " + this.deepestline + ",deepestcount is " + this.deepestcount);
		if(this.deepestcount>3 && this.deepestcount<this.s.length()-3){
			System.out.println(s.substring(this.deepestcount-3, this.deepestcount) + "    >>>" + s.substring(this.deepestcount, this.deepestcount+1) + "<<<   " + s.substring(this.deepestcount+1, this.deepestcount+4));
		}
	}
	
	public void revert(Source s){
		this.s = s.s;
		this.position = s.position;
		this.count = s.count;
		this.line = s.line;
				
	}
	
	@Override
	public Source clone(){
		Source s = new Source(this.s);
		s.position = this.position;
		s.line = this.line;
		s.count = this.count;
		s.deepestcount = this.deepestcount;
		s.deepestline = this.deepestline;
		return s;
	}
	
	//¬Œ÷Šm”F
	public void is_parsed(Summery summery){
		if(s.length() != position){
			summery.parse_faileds.add("parse failed : "  + summery.file.toString());
		}
	}
}
