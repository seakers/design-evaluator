//package vassar.matlab;
//
//
//
//import com.mathworks.engine.MatlabEngine;
//
//
//
//
//
//public class MatlabEngineWrapper {
//
//
//    public static MatlabEngine publicEngine = null;
//
//    public static MatlabEngine getEngine(){
//
//        if(MatlabEngineWrapper.publicEngine == null){
//            try{
//                MatlabEngineWrapper.publicEngine = MatlabEngine.connectMatlab();
//            }
//            catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//        return MatlabEngineWrapper.publicEngine;
//    }
//
//    public static MatlabEngine getNewEngine(){
//        MatlabEngine engine = null;
//
//
//        try{
//            engine = MatlabEngine.startMatlab(new String[]{"-nosplash"});
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//        return engine;
//    }
//
//
//
//
//
//    public static void eval(String command){
//        System.out.println("---> EVAL: " + command);
//
//        MatlabEngine engine = MatlabEngineWrapper.getNewEngine();
//
//        try{
//            engine.eval(command);
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    public static void feval(String functionName, Object args){
//        System.out.println("---> FEVAL: " + functionName);
//
//        MatlabEngine engine = MatlabEngineWrapper.getNewEngine();
//
//        try{
//            engine.feval(functionName, args);
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    public static Object[] returningFeval(String functionName, int nargout, Object args){
//        System.out.println("---> returningFeval: " + functionName + " " + nargout + " " + args.toString() + " " + args.getClass());
//
//        MatlabEngine engine = MatlabEngineWrapper.getNewEngine();
//
//        System.out.println("--> GOT ENGINE");
//
//        Object[] to_return = null;
//        try{
//
//            if(nargout == 1){
//                Object val = engine.feval(nargout, functionName, args);
//                to_return = new Object[]{val};
//            }
//            else if(nargout > 1){
//                Object[] val = engine.feval(nargout, functionName, args);
//                to_return = val;
//            }
//            else{
//
//            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }
//
//        return to_return;
//    }
//
//
//
//
////    public MatlabEngineWrapper(){
////
////        try{
////            this.engine = MatlabEngine.connectMatlab();
////        }
////        catch (Exception e){
////            e.printStackTrace();
////        }
////
////        if(this.engine == null){
////            System.out.println("---> CONSTRUCTOR COULDN'T MAKE MATLAB OBJECT");
////            System.exit(0);
////        }
////        else{
////            System.out.println("---> CONSTRUCTOR MADE SUCCESSFULLY");
////            System.exit(0);
////        }
////
////    }
//
//
//
//
//
//
//
//
//
//
//
//}
