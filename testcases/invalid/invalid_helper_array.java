class A{
    /*`@def_type NatArray = {int[] v | (\forall int i; 
        0 <= i && i < v.length; v[i] >= 0)};
    */

    int[]/*`@ refinement_type NatArray*/ nat_array;

    //@requires nat_array.length == 5;
    //@helper
    void test1(){
        nat_array[0] = -1;
        nat_array = new int[3];
    }

    //@requires nat_array.length == 5;
    //@helper
    void test2(int x){
        if(x > 0)nat_array[0] = -1;
        if(x > 0)nat_array = new int[3];
        if(x > 0)nat_array[0] = 1;
    }

    //@requires nat_array.length == 5;
    //@requires (\forall int i; 0 <= i && i < nat_array.length; nat_array[i] >= 0);
    //@helper
    void test3(int x){
        if(x > 0)nat_array[0] = -1;
        //@maintaining nat_array.length == 5;
        //@maintaining (\forall int i; 0 <= i && i < nat_array.length; (x >= 0 || i != 0) ==>  nat_array[i] >= 0);
        for(int i = -100; i < 100; i = i+1){
            nat_array = new int[5];
        }
        if(x > 0)nat_array[0] = 1;
    }

    //@requires nat_array.length == 5;
    //@helper
    void test2_m(int x){
        if(x > 0)nat_array[0] = -1;
        assign(x);
        if(x > 0)nat_array[0] = 1;
    }

    //@requires nat_array.length == 5;
    //@requires (\forall int i; 0 <= i && i < nat_array.length; nat_array[i] >= 0);
    //@helper
    void test3_m(int x){
        if(x > 0)nat_array[0] = -1;
        //@maintaining nat_array.length == 5;
        //@maintaining (\forall int i; 0 <= i && i < nat_array.length; (x <= 0 || i != 0) ==>  nat_array[i] >= 0);
        for(int i = -100; i < 100; i = i+1){
            assign(x);
        }
        if(x > 0)nat_array[0] = 1;
    }

    //@helper
    void test4(int[]/*`@ refinement_type NatArray*/ arg_array){
        int[]/*`@ refinement_type NatArray*/ local = new int[3];
        local[0] = -1;
        local = arg_array;
        local[0] = -1;
        local = arg_array;
    }

    //@requires arg_array != nat_array;
    //@helper
    void test4_m(int[]/*`@ refinement_type NatArray*/ arg_array){
        int[]/*`@ refinement_type NatArray*/ local = new int[3];
        hoge(local);
        local[0] = -1;
        local = arg_array;
    }

    //@requires x < 0;
    //@assignable \nothing;
    //@also
    //@requires x >= 0;
    //@ensures nat_array.length == 5;
    //@ensures (\forall int i; 0 <= i && i < nat_array.length; nat_array[i] >= 0);
    //@assignable nat_array;
    //@helper
    void assign(int x){
        if(x >= 0){
            nat_array = new int[5];
        }
        //@assert false;
    }

    //@helper pure
    void hoge(int[]/*`@ refinement_type NatArray*/ arg){
        int x = 3;
        //@assert false;
    }
}