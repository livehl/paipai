package tools

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javassist.ClassPool
import javassist.CtMethod
import javassist.Modifier
import javassist.bytecode.LocalVariableAttribute
import javassist.ClassClassPath
import java.util
import java.lang.reflect.Method

import common.SoftHashMap

/**
 * 功能:反射工具类
 *
 * @author 黄林 2011-10-27
 * @version
 */
object ReflectTool {
  lazy val cacheMap = new SoftHashMap[Class[AnyRef], util.HashMap[String, Method]]()

  /**
   * Gets the bytes by object.
   *
   * @param obj
   * the ser
   * @return the bytes by object
   * @throws Exception
   * the exception
   * @author 黄林
   */
  def getBytesByObject(obj: AnyRef) = {
    val baos = new ByteArrayOutputStream();
    val oos = new ObjectOutputStream(baos);
    oos.writeObject(obj);
    baos.toByteArray();
  }

  /**
   * Gets the object by bytes.
   *
   * @param data
   * the data
   * @return the object by bytes
   * @throws Exception
   * the exception
   * @author 黄林
   */
  def getObjectByBytes(data: Array[Byte]) = {
    val bais = new ByteArrayInputStream(data);
    val ois = new ObjectInputStream(bais);
    ois.readObject();
  }

  /**
   *
   * <p>
   * 获取方法参数名称
   * </p>
   *
   * @param cm
   * @return
   */
  protected def getMethodParamNamesByCtMethod(cm: CtMethod) = {
    val cc = cm.getDeclaringClass();
    val methodInfo = cm.getMethodInfo();
    val codeAttribute = methodInfo.getCodeAttribute();
    val attr = codeAttribute
      .getAttribute(LocalVariableAttribute.tag).asInstanceOf[LocalVariableAttribute];
    if (attr == null) {
      throw new Exception(cc.getName());
    }
    val pos = if (Modifier.isStatic(cm.getModifiers())) 0 else 1
    0 until cm.getParameterTypes().length map {
      i => attr.variableName(i + pos)
    } toList
  }

  /**
   * 获取方法参数名称，按给定的参数类型匹配方法
   *
   * @param clazz
   * @param method
   * @param paramTypes
   * @return
   */
  def getMethodParamNames(clazz: Class[_ <: AnyRef], method: String, paramTypes: Class[AnyRef]*) = {
    val pool = ClassPool.getDefault();
    val cc = pool.get(clazz.getName());
    val paramTypeNames = 0 until paramTypes.length map {
      i => paramTypes(i).getName()
    } toArray
    val cm = cc.getDeclaredMethod(method, pool.get(paramTypeNames));
    getMethodParamNamesByCtMethod(cm)
  }

  /**
   * 获取方法参数名称，匹配同名的某一个方法
   *
   * @param clazz
   * @param method
   * @return
   * 如果类或者方法不存在
   * 如果最终编译的class文件不包含局部变量表信息
   */
  def getMethodParamNamesByMethodName(clazz: Class[_ <: AnyRef], method: String) = {
    val pool = ClassPool.getDefault();
    val cc = pool.get(clazz.getName());
    val cm = cc.getDeclaredMethod(method);
    getMethodParamNamesByCtMethod(cm)
  }

  def getConstructorParamNames[T](clazz: Class[_ <: T]) = {
    val pool = ClassPool.getDefault();
    val cc = {
      val ctclass = pool.getOrNull(clazz.getName());
      if (ctclass == null) {
        pool.insertClassPath(new ClassClassPath(clazz));
        pool.get(clazz.getName())
      } else {
        ctclass
      }
    }
    val ccons = cc.getConstructors()
    val javaConstructors = clazz.getConstructors()
    0 until ccons.length map {
      i =>
        val ccon = ccons(i)
        val methodInfo = ccon.getMethodInfo();
        val codeAttribute = methodInfo.getCodeAttribute();
        val attr = codeAttribute
          .getAttribute(LocalVariableAttribute.tag).asInstanceOf[LocalVariableAttribute];
        if (attr == null) {
          throw new Exception(cc.getName());
        }
        val pos = if (Modifier.isStatic(ccon.getModifiers())) 0 else 1
        val parameterTypes = javaConstructors(i).getParameterTypes()
        javaConstructors(i) -> (0 until ccon.getParameterTypes().length map {
          i => attr.variableName(i + pos) -> parameterTypes(i)
        } toList)
    } toList
  }
}