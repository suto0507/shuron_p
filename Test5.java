package soturon;

public class Test5 {
	public static void main(String[] args) throws Exception {
		A a = new A();
		a.x = 5;
	}
	int hoge(){
		A a = new A();
		a.x = 5;
		return a.x;
	}
}

class A{
	final int x = 0;
}