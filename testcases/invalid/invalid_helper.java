class A{
    /*`@def_type DiffArray = {int[] v | (\forall int i,j; 
        0 <= i && i < v.length && 0 <= j && j < v.length; i != j ==> v[i] != v[j])};
    */

    /*`@ def_type Nat = {int x | x > 0};*/
	
	int/*`@ refinement_type Nat*/ x;

    int/*`@ refinement_type {int x | x > x2}*/ x1;
    final int/*`@ refinement_type {int x | x < 0}*/ x2;
    
    //@helper
    void test1(){
        x = x + 1;
        x = x - 2;
    }

    //@helper
    void test2(){
        x1 = 771;
    }

    //@requires array.length == 5;
    //@requires 0 < i1 && 0 < i2;
    //@requires array.length > i1 && array.length > i2;
    //@helper
    void swap(int i1, int i2, int[]/*`@refinement_type DiffArray*/ array){
        int tmp = array[i1];
        array[i1] = array[i2];
    }
}