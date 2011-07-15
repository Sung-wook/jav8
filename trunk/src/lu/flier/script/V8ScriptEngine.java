package lu.flier.script;

import java.io.IOException;
import java.io.Reader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public final class V8ScriptEngine extends AbstractScriptEngine implements Invocable, Compilable
{
    private final V8ScriptEngineFactory factory;

    V8ScriptEngine(V8ScriptEngineFactory factory)
    {
        assert factory != null;

        this.factory = factory;
        this.context = new V8Context();

        Bindings scope = getBindings(ScriptContext.ENGINE_SCOPE);

        scope.put(ENGINE, factory.getEngineName());
        scope.put(ENGINE_VERSION, factory.getEngineVersion());
        scope.put(NAME, factory.getName());
        scope.put(LANGUAGE, factory.getLanguageName());
        scope.put(LANGUAGE_VERSION, factory.getLanguageVersion());
    }
    
    private V8Context getV8Context()
    {
    	return (V8Context) this.context;
    }
        
    private String readAll(Reader reader) throws IOException
    {
    	StringBuilder sb = new StringBuilder();
    	
    	char[] buffer = new char[8192];
    	int read;

    	while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
		    sb.append(buffer, 0, read);
		}
			
		return sb.toString();
    }    
    
    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException
    {
    	if (script == null) throw new IllegalArgumentException("empty script");
    	    	
    	this.getV8Context().enter();
    	
        try {
			return new V8CompiledScript(this, this.getV8Context(), script).eval(context);
		} catch (Exception e) {
			throw new ScriptException(e);
		} finally {
			this.getV8Context().leave();
		}
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException
    {    	
        try {
			return eval(readAll(reader), context);
		} catch (IOException e) {
			throw new ScriptException(e);
		}
    }

    @Override
    public Bindings createBindings()
    {
    	return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory()
    {
        return this.factory;
    }    

	@Override
	public CompiledScript compile(String script) throws ScriptException 
	{		
		this.getV8Context().enter();
    	
		try {
			return new V8CompiledScript(this, this.getV8Context(), script);
		} catch (Exception e) {
			throw new ScriptException(e); 
		} finally {
			this.getV8Context().leave();
		}
	}

	@Override
	public CompiledScript compile(Reader script) throws ScriptException {
		try {
			return compile(readAll(script));
		} catch (IOException e) {
			throw new ScriptException(e);
		}
	}

	@Override
	public Object invokeMethod(Object thiz, String name, Object... args)
			throws ScriptException, NoSuchMethodException 
	{
		this.getV8Context().enter();
		try {
			return ((V8Function) ((V8Object) thiz).get(name)).invoke(args);
		} finally {
			this.getV8Context().leave();
		}
	}

	@Override
	public Object invokeFunction(String name, Object... args)
			throws ScriptException, NoSuchMethodException 
	{
		this.getV8Context().enter();
		try {
			V8Function func = (V8Function) getV8Context().getGlobal().get(name);
			
			if (func == null) throw new NoSuchMethodException(name);
			
			return func.invoke(args);
		} finally {
			this.getV8Context().leave();
		}
	}

	@Override
	public <T> T getInterface(Class<T> clasz) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T getInterface(Object thiz, Class<T> clasz) {
		// TODO Auto-generated method stub
		return null;
	}
	
}