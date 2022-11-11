class A{
    //`@ def_type NatArray2 = {int[][] x | x[0][0] >= 0};

    int[][]/*`@ refinement_type NatArray2*/ nat_array2;

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test1(){
        nat_array2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test2(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[3][3];
        if(x >= 0)local2 = nat_array2;
        if(x < 0)local2[0][0] = -1;
        if(x < 0)local2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test3(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[3][3];
        if(x >= 0)local2 = nat_array2;
        //@maintaining local2.length == 3;
        //@maintaining local2[0].length == 3;
        for(int i = -100; i < x && x < 0; i = i+1){
            local2[0][0] = -1;
        }
        if(x < 0)local2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@helper
    void test2_m(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[3][3];
        if(x >= 0)assign(x, local2);
        if(x < 0)local2[0][0] = -1;
        if(x < 0)local2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test3_m(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[3][3];
        //@maintaining local2.length == 3;
        //@maintaining local2[0].length == 3;
        //@maintaining nat_array2[0][0] >= 0;
        //@maintaining local2[0][0] >= 0;
        for(int i = -100; i < x && x >= 0; i = i+1){
            assign(x, local2);
        }
        if(x < 0)local2[0][0] = -1;
        if(x < 0)local2[0][0] = 1;
    }

    //@requires x < 0;
    //@assignable \nothing;
    //@also
    //@requires x >= 0;
    //@ensures nat_array2.length == 5;
    //@ensures nat_array2[0].length == 5;
    //@ensures nat_array2[0][0] >= 0;
    //@assignable nat_array2;
    //@helper
    void assign(int x, int[][]/*`@ refinement_type NatArray2*/ local2){
        if(x >= 0){
            nat_array2 = new int[5][5];
        }
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
            local2[0][0] = 1;
            local2 = nat_array2;
        }
        if(x <= 0)local2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test4_m(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[3][3];
        //@maintaining local2.length == 3;
        //@maintaining local2[0].length == 3;
        //@maintaining nat_array2[0][0] >= 0;
        //@maintaining local2[0][0] >= 0;
        for(int i = -100; i < x; i = i+1){
            if(x < 0)local2[0][0] = -1;
            if(x >= 0)assign(x, local2);
            if(x < 0)local2[0][0] = 1;
        }
        if(x <= 0)local2[0][0] = 1;
    }

    //@requires nat_array2.length == 5;
    //@requires nat_array2[0].length == 5;
    //@requires nat_array2[0][0] >= 0;
    //@helper
    void test5_m(int x){
        int[][]/*`@ refinement_type NatArray2*/ local2 = new int[3][3];
        //@maintaining local2.length == 3;
        //@maintaining local2[0].length == 3;
        //@maintaining nat_array2[0][0] >= 0;
        for(int i = -100; i < x; i = i+1){
            local2[0][0] = 1;
            assign(x, local2);
        }
        if(x <= 0)local2[0][0] = 1;
    }
}