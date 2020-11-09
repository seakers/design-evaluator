package vassar.matlab;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.MatlabProxyFactory;



public class MatlabProxyFactoryWrapper {

    public MatlabProxyFactory factory;



    public static MatlabProxyFactory publicFactory = null;


    public static MatlabProxy getPublicProxy(){
        if(MatlabProxyFactoryWrapper.publicFactory.equals(null)){
            MatlabProxyFactoryOptions options       = new MatlabProxyFactoryOptions.Builder()
                    .setHidden(true)
                    .setUsePreviouslyControlledSession(true).setUseSingleComputationalThread(true)
                    .build();
            MatlabProxyFactoryWrapper.publicFactory = new MatlabProxyFactory(options);
        }
        try{
            return MatlabProxyFactoryWrapper.publicFactory.getProxy();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public MatlabProxyFactoryWrapper(){
        if(MatlabProxyFactoryWrapper.publicFactory == null){
            MatlabProxyFactoryOptions options       = new MatlabProxyFactoryOptions.Builder()
                    .setHidden(true)
                    .setUsePreviouslyControlledSession(true).setUseSingleComputationalThread(true)
                    .build();
            MatlabProxyFactoryWrapper.publicFactory = new MatlabProxyFactory(options);
        }
    }

    public MatlabProxy getProxy(){
        return MatlabProxyFactoryWrapper.getPublicProxy();
    }




}
