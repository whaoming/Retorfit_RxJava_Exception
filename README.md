# Retorfit_RxJava_Exception
优雅地处理服务器返回的错误和客户端访问网络过程中产生的错误  

ps：可以看看我关于Retrofit+RxJava第二篇文章,其中主要讲了一些封装与业务逻辑的处理  
> 优雅地使用Retrofit+RxJava(二)  
> http://blog.csdn.net/qq122627018/article/details/68957782    

## 前言

发现我的bolg中很大部分地博文都是在讲MVP模式，也不是特意去写，因为前段时间看了一篇关于MVP结构的文章，然后第二天在敲自己项目的时候，就被那篇文章的思路带着走了，发现自己项目的结构还停留在MVC中。
正好昨晚隔壁宿舍的童鞋问我在MVP中怎么处理各种错误信息，我当时跟他口头讲发现并没有那么容易，所以就有这篇blog了
## 异常/错误的产生

其实在这里异常和错误是应该分开讲的，就好比如说密码出现空格之类，或者说账号密码出错等等称之为错误，然后比如网络断开或者不稳定等称之为异常。那么这里为什么要放在一起说呢？因为对于有强迫症的lz来说，希望有一个类似于异常引擎(ExceptionEngine)的东西，可以去处理所有异常或者错误信息。前面我写过一篇文章：

> Retrofit-自定义异常处理工厂(一)
> http://blog.csdn.net/qq122627018/article/details/51540812

里面提出了异常产生的地方，在这里我再拿出来说一下
在使用Retrofit+RxJava访问RestFul服务器的过程中，总免不了要处理错误，错误有很多种，例如服务器端的错误：密码错误，cookie过期等等。客户端中产生的错误：网络异常，解析数据出错等。但是这里就会出现一个问题，假如服务器返回的是统一数据格式：

```
/**
 * 公共数据格式
 * @author Mr.W
 * @param <T>
 */
public class Result<T> {
	public int state;
	public String error;
	public T infos;

}
```
那么此时，如果是网络错误等客户端错误，在使用Retrofit+RxJava时会直接调用subscribe的onError事件，但是如果是服务器端的错误，比如说密码错误等，此时还是会执行subscribe的onnext事件，而且必须在subscribe里面处理异常信息，这对于一个对架构有强迫症的人是难以忍受的，就好像下面的代码：

```
UserEngine.initUserInfo("username", "password")
                .subscribe(new Observer<Result<Login>>() {
                    @Override
                    public void onCompleted() {
                        
                    }

                    @Override
                    public void onError(Throwable e) {
                            
                    }

                    @Override
                    public void onNext(Result<Login> data) {
                        if(data.state == 200){
                            //.....
                        }else if(data.state == 404){
                            
                        }
                    }
                });
```
现在我希望在发生任何错误的情况下，都会调用onError事件，并且由model来处理错误信息。那么，此时我们就应该有一个ExceptionEngine来处理事件流中的错误信息了。
## 在工作流中处理异常

在Retrofit+RxJava的框架下，我们获取网络数据的流程通常如下：
**订阅->调用Service->数据解析->ui展示**
整个数据请求过程都是发生在Rx中的工作流之中。**当有异常产生的时候，我们要尽量不在ui层里面进行判断，换句话说，我们没有必要去告诉ui层具体的错误信息，只需要让他弹出一个dialog展示我们给它的信息就行**。
### 思路

1.因为在*调用service*这一层发生错误时Retrofit会抛出异常，所以这一层我们暂时不去理会。
2.那么在数据解析这一层，我们必须设置一个拦截器，判断Result里面的code是否为成功，如果不成功，则要根据与服务器约定好的错误码来**抛出**对应的异常
3.通过上面俩层的处理，我们已经成功把服务器的错误信息也能通过Rx的onError事件展示出来，但是这还不够，我们要尽量做到不让ui层"知道太多",所以在经过这俩层之后我们必须设置一个拦截器，拦截onError事件，然后再根据具体的错误信息来分发事件(这时候RxJava提供的操作符就派上用场了)
### 数据解析层的拦截器

```
 public Observable<WeatherBean> getWeather(String cityName){
        return weatherService.getWeather(cityName)
			    //拦截服务器返回的错误
                .map(new ServerResultFunc<WeatherBean>())
                //HttpResultFunc（）为拦截onError事件的拦截器，后面会讲到，这里先忽略
                .onErrorResumeNext(new HttpResultFunc<WeatherBean>());
    }
```

```
	//拦截固定格式的公共数据类型Result<T>,判断里面的状态码
    private class ServerResultFunc<T> implements Func1<Result<T>, T> {
        @Override
        public T call(Result<T> httpResult) {
	        //对返回码进行判断，如果不是0，则证明服务器端返回错误信息了，便根据跟服务器约定好的错误码去解析异常
            if (httpResult.errNum != 0) {
            //如果服务器端有错误信息返回，那么抛出异常，让下面的方法去捕获异常做统一处理
                throw new ServerException(httpResult.errNum,httpResult.errMsg);
            }
            //服务器请求数据成功，返回里面的数据实体
            return httpResult.retData;
        }
    }

```

所以整个逻辑是这样的：
![这里写图片描述](http://img.blog.csdn.net/20160616133223975)
所以在前三步的过程中，只要发生异常(服务器返回的错误也抛出了)都会抛出，这时候就触发了RxJava的OnError事件，那么在上面我们也提出了，不能让ui层"知道太多",所以此时，我们需要另外一个拦截器

### 一个拦截onError事件的拦截器

在第四步中，我们就必须提供一个可供view显示的error事件了。什么叫可供view显示呢？就是不必提供具体错误信息，让用户"能看懂"的错误提示。
拦截onError事件：
```
 private class HttpResultFunc<T> implements Func1<Throwable, Observable<T>> {
        @Override
        public Observable<T> call(Throwable throwable) {
		    //ExceptionEngine为处理异常的驱动器
            return Observable.error(ExceptionEngine.handleException(throwable));
        }
    }
```
所以在一整个流中，我们完整的代码是这样的：

```
 public Observable<WeatherBean> getWeather(String cityName){
        return weatherService.getWeather(cityName)
			    //拦截服务器返回的错误
                .map(new ServerResultFunc<WeatherBean>())
                //HttpResultFunc（）为拦截onError事件的拦截器，后面会讲到，这里先忽略
                .onErrorResumeNext(new HttpResultFunc<WeatherBean>());
    }
```
## 处理异常的驱动器

那么这个驱动器的作用是什么呢？在上面的工作流中，最后的一个拦截器就拦截了工作流中的onError事件，这个onError事件有可能是在请求网络的过程中发出的，也有可能是第一个拦截器在解析服务器返回数据的时候发现错误而发出的。那么我们这个驱动器就负责判断并处理onError事件里面的错误，然后发出一个"能让view层知道"的错误信息
### view层的异常类

```
/**
 * Created by 12262 on 2016/5/31.
 * view层自定义异常类
 */
public class ApiException extends Exception {
    private int code;
    //用于展示的异常信息
    private String displayMessage;

    public ApiException(Throwable throwable, int code) {
        super(throwable);
        this.code = code;

    }

    public void setDisplayMessage(String displayMessage) {
        this.displayMessage = displayMessage;
    }

    public String getDisplayMessage() {
        return displayMessage;
    }

    public int getCode() {
        return code;
    }
}
```
### 异常驱动器核心

```
**
 * Created by 12262 on 2016/5/30.
 */
public class ExceptionEngine {

    //对应HTTP的状态码
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int NOT_FOUND = 404;
    private static final int REQUEST_TIMEOUT = 408;
    private static final int INTERNAL_SERVER_ERROR = 500;
    private static final int BAD_GATEWAY = 502;
    private static final int SERVICE_UNAVAILABLE = 503;
    private static final int GATEWAY_TIMEOUT = 504;

    public static ApiException handleException(Throwable e){
        ApiException ex;
        if (e instanceof HttpException){             //HTTP错误
            HttpException httpException = (HttpException) e;
            ex = new ApiException(e, ERROR.HTTP_ERROR);
            switch(httpException.code()){
                case UNAUTHORIZED:
                case FORBIDDEN:
                case NOT_FOUND:
                case REQUEST_TIMEOUT:
                case GATEWAY_TIMEOUT:
                case INTERNAL_SERVER_ERROR:
                case BAD_GATEWAY:
                case SERVICE_UNAVAILABLE:
                default:
                    ex.setDisplayMessage("网络错误");  //均视为网络错误
                    break;
            }
            return ex;
        } else if (e instanceof ServerException){    //服务器返回的错误
            ServerException resultException = (ServerException) e;
            ex = new ApiException(resultException, resultException.getCode());
            ex.setDisplayMessage(resultException.getMsg());
            return ex;
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof ParseException){
            ex = new ApiException(e, ERROR.PARSE_ERROR);
            ex.setDisplayMessage("解析错误");            //均视为解析错误
            return ex;
        }else if(e instanceof ConnectException){
            ex = new ApiException(e, ERROR.NETWORD_ERROR);
            ex.setDisplayMessage("连接失败");  //均视为网络错误
            return ex;
        }else {
            ex = new ApiException(e, ERROR.UNKNOWN);
            ex.setDisplayMessage("未知错误");          //未知错误
            return ex;
        }
    }
}
```

到这里，我们就基本完成了我们最开始的目的。在事件流中截取错误并做统一的分发。所以在view中，我们只要把onError事件中的throeable强转称ApiException就行(当然也可以不用每个订阅者都做同样的事，可以参考下面MyObserver的代码)
## view层最最简单地示范


```
 btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpMethods.getInstance().getWeather("北京")
                        .subscribe(new SampleProgressObserver<WeatherBean>(MainActivity.this) {
                            @Override
                            public void onNext(WeatherBean weatherBean) {
                                tv.setText(weatherBean.toString());
                            }
                        });
            }
        });
```
demo截图：
![这里写图片描述](http://img.blog.csdn.net/20160616142723594)![这里写图片描述](http://img.blog.csdn.net/20160616142743876)



**ps**：关于SampleProgressObserver我就不说了哈，这里po下代码,对RxJava了解的人应该都能看懂的

```
/**
 * Created by 12262 on 2016/6/5.
 */
public abstract class SampleProgressObserver<T> extends MyObserver<T>{
    private LoadingDialog dialog;
    private AlertDialog msgDialog;
    private Context context;

    public SampleProgressObserver(Context context) {
        dialog = new LoadingDialog(context).builder().setMessage("正在加载中");
        this.context = context;
    }

    @Override
    public void onStart() {
        dialog.show();
        super.onStart();
    }

    @Override
    protected void onError(ApiException ex) {
        ex.printStackTrace();;
        dialog.dismiss();
        msgDialog = new AlertDialog.Builder(context, R.style.MingDialog).setMessage(ex.getDisplayMessage()).setPositiveButton("确定", null).create();
        msgDialog.show();
    }

    @Override
    public void onCompleted() {
        dialog.dismiss();
    }

}
```

```
/**
 * Created by 12262 on 2016/5/30.
 */
public abstract class MyObserver<T> extends Subscriber<T> {

    @Override
    public void onError(Throwable e) {
        if(e instanceof ApiException){
            onError((ApiException)e);
        }else{
            onError(new ApiException(e,123));
        }
    }

    /**
     * 错误回调
     */
    protected abstract void onError(ApiException ex);
}

```

> BLOG
> http://blog.csdn.net/qq122627018/article/details/51689891#t8


