package soturon;

public class A {
	B b;
	A(){
		b = new B(this);	
	}
	void set(B b_arg){
		b = b_arg;
	}
	int get(){
		return b.x;
	}
	public static void main(String[] args){
		A a = new A();
	}
}

class B{
	final int x;
	B(A a){
		a.set(this);
		System.out.print(a.get());
		int num = a.get();
		x = 3;
	}
}
