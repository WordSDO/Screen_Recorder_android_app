package com.hbisoft.hbrecorder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class FileObserver extends android.os.FileObserver {
    private int mMask;
    private List<SingleFileObserver> mObservers;
    private final String mPath;
    private final MyListener ml;


    FileObserver(String path, MyListener myListener) {
        super(path, ALL_EVENTS);
        mPath = path;
        mMask = ALL_EVENTS;
        this.ml = myListener;
    }


    public void startWatching() {
        if (mObservers == null) {
            mObservers = new ArrayList();
            Stack stack = new Stack();
            stack.push(mPath);
            while (!stack.isEmpty()) {
                String parent = (String) stack.pop();
                mObservers.add(new SingleFileObserver(parent, mMask));
                File[] listFiles = new File(parent).listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
                            stack.push(file.getPath());
                        }
                    }
                }
            }
            for (SingleFileObserver startWatching : mObservers) {
                startWatching.startWatching();
            }
        }
    }

    public void stopWatching() {
        if (mObservers != null) {
            for (SingleFileObserver stopWatching : mObservers) {
                stopWatching.stopWatching();
            }
            mObservers.clear();
            mObservers = null;
        }
    }


    @Override
    public void onEvent(int event, final String path) {
        if (event == android.os.FileObserver.CLOSE_WRITE) {
            ml.callback();
        }
    }

    static class SingleFileObserver extends android.os.FileObserver {
        final String mPath;


        SingleFileObserver(String path, int mask) {
            super(path, mask);
            mPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            String newPath = mPath + "/" + path;
            onEvent(event, newPath);
        }
    }
}
