package dev.cowclient.core;

import java.util.ArrayDeque;
public final class ClickCounter {
    private final ArrayDeque<Long> left=new ArrayDeque<>(), right=new ArrayDeque<>();
    public void click(int button,long now) {
        if(button==0) left.addLast(now);
        else if(button==1) right.addLast(now);
        prune(now);
    }
    private void prune(long now) {
        while(!left.isEmpty() && now-left.peekFirst()>=1000) left.removeFirst();
        while(!right.isEmpty() && now-right.peekFirst()>=1000) right.removeFirst();
    }
    public int left(long now) { prune(now); return left.size(); }
    public int right(long now) { prune(now); return right.size(); }
}
