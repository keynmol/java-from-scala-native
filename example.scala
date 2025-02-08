import libjni.all.*
import scalanative.unsafe.*
import constants.*

@main def hello =
  Zone:
    // Part 1: initialising basic JNI interface
    val iface = libjni.structs.JNIInvokeInterface_()
    val args  = JavaVMInitArgs()
    (!args).version = jint(0x00010008) // JNI_VERSION_1_8

    val customClasspath =
      sys.env
        .get("CLASSPATH")
        .map(_.trim)
        .orElse:
          val path = os.Path("./CLASSPATH", os.pwd)
          Option.when(os.exists(path)):
            os.read(path).trim

    customClasspath match
      case None =>
        (!args).nOptions = jint(0)
      case Some(value) =>
        val cp = "-Djava.class.path=" + value
        (!args).options = JavaVMOption(toCString(cp), null)
        (!args).nOptions = jint(1)

    val hasCustomClasspath = customClasspath.nonEmpty

    val vm  = doublePointer(JavaVM(iface))
    val env = doublePointer[JNIEnv](JNIEnv(null))

    val res = JNI_CreateJavaVM(
      vm,
      env.asInstanceOf[Ptr[Ptr[Byte]]],
      args.asInstanceOf[Ptr[Byte]],
    )
    if res.value != JNI_OK then sys.error("Failed to create Java VMn")

    // look at this shit
    val jvm: JNINativeInterface_ = (!(!(!env)).value)

    // Part 2: using JNI interface to invoke built-in Java methods. Cann you guess which ones?
    val system = jvm.FindClass(!env, c"java/lang/System")
    assert(system.value != null, "Failed to find java.lang.System class")

    val outField =
      jvm.GetStaticFieldID(!env, system, c"out", c"Ljava/io/PrintStream;");
    assert(outField.value != null, "Failed to find .out field on System")

    val out = jvm.GetStaticObjectField(!env, system, outField)
    assert(out.value != null)

    val printStream = jvm.GetObjectClass(!env, out)
    assert(printStream.value != null)

    val printlnMethod =
      jvm.GetMethodID(!env, printStream, c"println", c"(Ljava/lang/String;)V")
    assert(printlnMethod.value != null)

    val str =
      jvm.NewStringUTF(!env, c"Hello world from Java from... Scala Native?..")

    val arguments = va_list(toCVarArgList(str))

    jvm.CallVoidMethodV(!env, out, printlnMethod, arguments)

    // Part 3: now let's invoke a println method from Scala predef!
    if hasCustomClasspath then
      val scalaPredef = jvm.FindClass(!env, c"scala/Predef")
      assert(scalaPredef.value != null)

      val otherHello = jvm.NewStringUTF(
        !env,
        c"Hello world from Scala from JNI from Scala Native?...",
      )

      val printlnMethod = jvm.GetStaticMethodID(
        !env,
        scalaPredef,
        c"println",
        c"(Ljava/lang/Object;)V",
      )
      assert(printlnMethod.value != null)

      jvm.CallStaticObjectMethodV(
        !env,
        scalaPredef,
        printlnMethod,
        toCVarArgList(otherHello),
      )

    end if

// look at this shit
inline def doublePointer[A: Tag](value: A) =
  val ptr1 = stackalloc[A]()
  val ptr2 = stackalloc[Ptr[A]]()
  ptr2.update(0, ptr1)(using Tag.materializePtrTag[A])
  !ptr1 = value

  ptr2

inline def doubleOpaquePointer[A: Tag](value: A) =
  doublePointer(value).asInstanceOf[Ptr[Ptr[Byte]]]

// these are macros in C and cannot be generate by bindgen, so we define them manually
object constants:
  val JNI_OK        = 0    /* success */
  val JNI_ERR       = (-1) /* unknown error */
  val JNI_EDETACHED = (-2) /* thread detached from the VM */
  val JNI_EVERSION  = (-3) /* JNI version error */
  val JNI_ENOMEM    = (-4) /* not enough memory */
  val JNI_EEXIST    = (-5) /* VM already created */
  val JNI_EINVAL    = (-6) /* invalid arguments */
