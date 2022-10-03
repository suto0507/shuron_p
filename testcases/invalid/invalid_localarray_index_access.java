class A{
    //`@ def_type NatArray = {int[] x | (\forall int i; 0 <= i && i < x.length; x[i] >= 0)};
    int[] /*`@refinement_type NatArray*/ x;

    //`@ def_type NatArray2 = {int[][] x | (\forall int i,j; 0 <= i && i < x.length && 0 <= j && j < x[i].length; x[i][j] >= 0)};
    int[][] /*`@refinement_type NatArray2*/ x2;

    //@ requires x.length == 3;
    void test7(){
        int[] local;
        local = new int[3];
        local = x;
        local[0] = 1;
    }

    //@ requires x.length == 3;
    void test8(){
        int[][] local2;
        local2 = new int[2][3];
        local2[1] = x;
        local2[1][1] = 2;
    }


}