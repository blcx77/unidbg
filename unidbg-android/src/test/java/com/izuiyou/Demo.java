package com.izuiyou;

class A {
    public static final String name = "class A";
};

class B extends A {
    public static final String name = "class B";
};


public class Demo {
    public void test(A obj) {
        System.out.println(obj.getClass().getSimpleName());
    }


    public static void main(String[] args) {
        Demo d = new Demo();
        d.test(new A());
        d.test(new B());
    }
}
