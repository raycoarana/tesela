Tesela
======

Tesela is a code generate based library that helps you implement Model-View-Presenter pattern
on Android. It let you annotate methods to make them be executed on UI thread or on a background
thread. It will help you to keep a weak reference to the view, so forget about leaking your
Fragment or Activity anymore while your background work finishes.

How to use it?
--------------

The first thing to do is to initialize Tesela providing it a TeselaEecutor implementation.
That way you can control where background work will be executed, how many threads are created,
use an standard Java thread pool or other kind of thread pool.

```java
public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final Handler mainThreadHandler = new Handler();
        final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
        Tesela.init(new TeselaExecutor() {
                    
                        @Override
                        public boolean isMainCurrentThread() {
                            return Looper.myLooper() == Looper.getMainLooper();
                        }
                    
                        @Override
                        public void executeInUIThread(Runnable runnable) {
                            mainThreadHandler.post(runnable);
                        }
                    
                        @Override
                        public void executeInBackground(Runnable runnable) {
                            executeInBackground(runnable, null);
                        }
                    
                        @Override
                        public void executeInBackground(Runnable runnable, String tag) {
                            backgroundExecutor.execute(runnable);
                        }
                    
                    });
    }

}
```

Once you have Tesela initialized, you just have to annotate your classes, the first thing is annotate
your view interface with the @View annotation.

```java
@View
public interface HelloWorldView {
    void showMessage(String message);
}
```

Then on your presenter, annotate your view attribute and change it's type to WeakXXXX, where
XXXX is the name of the view interface. This class will hold a weak reference to your view
and will have all methods of your interface, so you don't have to deal with the weak reference.

```java
public class HelloWorldPresenter {

    @ViewReference
    protected WeakHelloWorldView mView;

    public void onInitialize(HelloWorldView view) {
        mView = WeakHelloWorldView.of(view);
    }
    
...    
```

Now simply annotate methods that needs to be executed on a background thread with @Background
and methods that needs to be executed in UI thread with @UI.

```java
    @Background
    protected void onDoSomething() {
        //Do some hard work here

        refreshView();
    }

    @UI
    protected void refreshView() {
        mView.showMessage("HelloWorld");
    }
```

Finally in your DI framework make sure every time you inject your presenter, the generated
presenter by Tesela is injected instead.

```java
    @Provides
    protected HelloWorldPresenter provideHelloWorldPresenter(TeselaHelloWorldPresenter impl) {
        return impl;
    }
```

Take a look at the Sample project!

Download
--------

Download via Gradle:
```groovy
compile 'com.raycoarana.tesela:tesela-library:0.0.2'
apt 'com.raycoarana.tesela:tesela-compiler:0.0.2'
```

License
-------

    Copyright 2016 Rayco Araña

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.