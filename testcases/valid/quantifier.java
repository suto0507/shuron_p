class quantifier {
	
	/*@ ensures (\forall int i,j; 0<i && i<10 && 0<j && j < 10; 0<i+j && i+j<19);*/
	/*@ ensures (\exists int i,j; 0<i && i<10 && 0<j && j < 10; i+j == 18);*/
	void test(){
		int x;
	}

}