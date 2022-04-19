package soturon;

public class Test4 {
	B b;
	Test4(){
		b = new B(this);
		
	}
	int get(){
		return b.b1;
	}
	public static void main(String[] args) throws Exception {
		//Test4 t4 = new Test4();
		B b = new B();
	}
}

class B{
	final int b1;
	int g;
	B(){
		final int x = this.three();
		System.out.println(x);
		if(x > 0){
			b1 =0;
		}else{
			b1 = 3;
		}
		//for(int i = 0; i<5;i++){
		//	b1 = 5;
		//}
		System.out.println(b1);
		//x = 3;
		System.out.println(g);
		int num;
		//System.out.println(num);
		b1 = 2;
	}
	
	B(Test4 t4){
		System.out.println("po"+t4.get());
		b1 = 4;
		g = 3;
		System.out.println("yo"+t4.get());
	}
	
	int three(){
		return 3 + b1;
	}
	
	int ho(int x){
		if(x>0){
			return x;
		}else{
			
		}
	}
	
}