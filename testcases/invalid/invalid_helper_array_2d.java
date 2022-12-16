class A{
    //`@ def_type NatArray2 = {int[][] x | x[0][0] >= 0};
    int[][]/*`@ refinement_type NatArray2*/ nat_array2;

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test1(){
        nat_array2[0][0] = -1;
        nat_array2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test2(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[3][3];
        if(x >= 0)local2 = nat_array2;
        if(x <= 0)local2[0][0] = -1;
        if(x <= 0)local2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test3(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[5][5];
        if(x >= 0)local2 = nat_array2;

        //@maintaining local2.length == 5;
        //@maintaining local2[0].length == 5;
        for(int i = -100; i < 100 && x <= 1000; i = i+1){
            local2[0][0] = -1;
        }
        if(x <= 0)local2[0][0] = 1;
    }


    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test4(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[5][5];
        //@maintaining local2.length == 5;
        //@maintaining local2[0].length == 5;
        //@maintaining nat_array2.length == 5;
        //@maintaining nat_array2[0].length == 5;
        //@maintaining nat_array2[0][0] >= 0;
        for(int i = -100; i < x && x <= 0; i = i+1){
            local2[0][0] = -1;
            local2 = nat_array2;
        }
        if(x <= 0)local2[0][0] = 1;
    }

}