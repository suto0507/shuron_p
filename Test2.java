package soturon;

public class Test2 {
	
	private int num;
	Test2 t2;
	
	
	private Test2(){
		num = 771;
	}
	
	
	public static void main(String[] args) throws Exception {
		int[] arr = new int[4];
		System.out.println(arr[2]);
		Test2[] arrt2 = new Test2[4];
		//System.out.println(arrt2[2].num);
		System.out.println(new Test2().mult1(2,3,4));
		System.out.println(10 / 2 % 4 * 11 / 3);
	}
	
	void hoge(){
		t2.num = 5;
	}
	
	class A {
		int x;
		void neko(){
			Test2 t2 =new Test2();
		}
	}
	
	int mult1(final int x, final int y, final int z){
        int num = 0;
        /*@maintainning 0 <= i_1 && i_i <= x;*/
        /*@maintainning num = i_1 * y * z;*/
        for(int i_1 = 0; i_1 < x; i_1 = i_1 + 1){
            /*@maintainning 0 <= i_1 && i_i <= x;*/
            /*@maintainning 0 <= i_2 && i_2 <= y;*/
            /*@maintainning num = i_1 * y * z; + i_2 * z*/
            for(int i_2 = 0; i_2 < y; i_2 = i_2 + 1){
                /*@maintainning 0 <= i_1 && i_1 <= x;*/
                /*@maintainning 0 <= i_2 && i_2 <= y;*/
                /*@maintainning 0 <= i_3 && i_3 <= z;*/
                /*@maintainning num = i_1 * y * z; + i_2 * z + i_3;*/
                for(int i_3 = 0; i_3 < z; i_3 = i_3 + 1){
                    num = num + 1;
                }
            }
        }
        return num;
    }
	
	class B{
		final int b1;
		B(){
			int x = b1;
			b1 = 2;
		}
	}
}
