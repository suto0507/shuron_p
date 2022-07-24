class quantifier {
	
	/*@ ensures (\forall int i,j; 0<i && i<10 && 0<j && j < 10; 0<i+j && i+j<20);*/
	/*@ ensures (\exists int i,j; 0<i && i<10 && 0<j && j < 10; i+j == 19);*/
	void test(){
		int x;
	}

}