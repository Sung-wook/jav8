package lu.flier.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

public class V8ScriptEngineTest
{
    private ScriptEngine eng;

    @Before
    public void setUp()
    {
        this.eng = new ScriptEngineManager().getEngineByName("jav8");
    }

    @Test
    public void testV8ScriptEngineFactory()
    {    	
    	assertNotNull(this.eng);
    	
        ScriptEngineFactory factory = this.eng.getFactory();

        assertNotNull(factory);

        List<String> names = factory.getNames();

        assertTrue(names.contains("jav8"));
        assertTrue(names.contains("javascript"));

        List<String> mimeTypes = factory.getMimeTypes();

        assertTrue(mimeTypes.contains("text/javascript"));

        List<String> extensions = factory.getExtensions();

        assertTrue(extensions.contains("js"));
        
        assertEquals("Google V8", factory.getEngineName());
        assertEquals("ECMAScript", factory.getLanguageName());
        assertEquals("1.8.5", factory.getLanguageVersion());
        
        assertEquals("obj.say(Hello,World)", factory.getMethodCallSyntax("obj", "say", "Hello", "World"));
        assertEquals("print(\"Hello world\")", factory.getOutputStatement("Hello world"));
        assertEquals("s1;s2;", factory.getProgram("s1", "s2"));
    }
    
    @Test
    public void testCompile() throws ScriptException
    {
    	Compilable compiler = (Compilable) this.eng;
    	
    	CompiledScript script = compiler.compile("1+2");
    	
    	assertNotNull(script);
    	
    	assertEquals(this.eng, script.getEngine());
    	assertEquals(3, script.eval());
    	
    	script = compiler.compile(new StringReader("2+3"));
    	assertEquals(5, script.eval());
    }
    
    @Test
    public void testInvoke() throws ScriptException, NoSuchMethodException, IOException 
    {
    	Invocable invocable = (Invocable) this.eng; 
    	
    	this.eng.eval("function hello(name) { return 'Hello ' + name; }" + 
    				  "function Person(me) { this.say = function (you) { return me + ' say, hello ' + you; } }" +
    				  "var me = new Person('Flier');");
    	    	
    	assertEquals("Hello Flier", invocable.invokeFunction("hello", "Flier"));
    	
    	try {
    		invocable.invokeFunction("nonexist");
    		fail();
    	} catch (NoSuchMethodException e) {    		
    	}
    	
    	V8Context ctxt = ((V8ScriptEngine) this.eng).getV8Context();
    	
    	ctxt.enter(); 
    	try {
    		V8Object me = (V8Object) ctxt.getGlobal().get("me");
    		
    		assertNotNull(me);
    	
    		assertEquals("Flier say, hello baby", invocable.invokeMethod(me, "say", "baby"));
    	} finally {
    		ctxt.leave();
    	} 
    }    
    
    @Test
    public void testGlobal() throws ScriptException
    {
    	V8Context ctxt = ((V8ScriptEngine) this.eng).getV8Context();
    	
		this.eng.eval("var b = true;" +
				  "var i = 123;" +
				  "var n = 3.14;" +
				  "var s = 'test';" +
				  "var n1 = null;" +
				  "var n2 = undefined;" +
				  "var d = new Date();" +
				  "var o = { name: 'Flier' };" +
				  "var a = [1, 2, 3];" + 
				  "var f = function () {};");
    	
    	ctxt.enter(); 
    	try {    		
    		V8Object g = ctxt.getGlobal();
    		
    		assertNotNull(g);
    		assertEquals(10, g.size());
    		assertFalse(g.isEmpty());
    		
    		assertTrue(g.containsKey("b"));
    		assertFalse(g.containsKey("nonexist"));
    		assertTrue(g.containsValue("test"));
    		assertFalse(g.containsValue("nonexist"));
    		
    		assertEquals(true, g.get("b"));
    		assertEquals(123, g.get("i"));
    		assertEquals(3.14, g.get("n"));
    		assertEquals("test", g.get("s"));
    		assertEquals(null, g.get("n1"));
    		assertEquals(null, g.get("n2"));
    		assertEquals(Date.class, g.get("d").getClass());    		
    		assertEquals(V8Object.class, g.get("o").getClass());
    		assertEquals("Flier", ((Bindings) g.get("o")).get("name"));
    		assertEquals(V8Array.class, g.get("a").getClass());
    		assertEquals(V8Function.class, g.get("f").getClass());
    		
    		assertEquals(null, g.get("test"));
    		assertEquals(null, g.put("test", "test"));
    		assertEquals("test", g.get("test"));
    		assertEquals("test", g.remove("test"));
    		assertEquals(null, g.get("test"));
    		
    		g.clear();
    		assertTrue(g.isEmpty());
    	} finally {
    		ctxt.leave();
    	} 
    }
}