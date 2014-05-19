package io.higgs.common.annotations;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract annotation processor which, when extended allows the discovery of classes annotated with the
 * types configured in the sub-class.
 * <p/>
 * Each class discovered will have an SPI file generated and hence make the class discoverable using the
 * standard Java SPI. e.g. If an annotation/interface/class called com.domain.Test is discovered
 * then a file is generated in META-INF/services with name com.domain.Test and it's contents will be the
 * full qualified class path of the discovered class. i.e. if an implementation named com.domain.TestImpl is found
 * the file will contain com.domain.TestImpl. There will be one line in the file for each type that is found
 * So if we find a second implementation com.domain.TestImpl2 the file will have both, one on each line.
 */
public abstract class HiggsAnnotationProcessor extends AbstractProcessor {
    protected Set<Class<?>> indexedTypes = new HashSet<>();

    protected Types types;
    protected Filer filer;
    protected Elements elementUtils;
    protected Messager messager;
    protected Map<String, Set<String>> discovered = new HashMap<>();
    protected String prefix = "META-INF/services/";

    public HiggsAnnotationProcessor() {
        for (Class<?> klass : types()) {
            indexedTypes.add(klass);
        }
    }

    /**
     * @return A set set of types this processor should try to discover
     */
    protected abstract Set<Class<?>> types();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        types = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (Element element : roundEnv.getRootElements()) {
                if (!(element instanceof TypeElement)) {
                    return false;
                }
                TypeElement el = (TypeElement) element;
                messager.printMessage(Diagnostic.Kind.NOTE, "Discovering : " + el.getQualifiedName().toString());
//                ElementKind kind = element.getKind();
//                if (kind != ElementKind.ANNOTATION_TYPE) {
//                    return false;
//                }
                for (Class<?> t : indexedTypes) {
                    if (t.isAnnotation()) {
                        Class<? extends Annotation> t2 = (Class<? extends Annotation>) t;
                        Annotation annotation = el.getAnnotation(t2);
                        if (annotation == null) {
                            continue;
                        }
                        //index the annotation
                        indexType(t.getName(), el);
                    } else {
                        //check if el extends or implements t, if so then generate the SPI file
                        TypeElement expected = elementUtils.getTypeElement(t.getName());
                        if (processingEnv.getTypeUtils().isAssignable(expected.asType(), el.asType())) {
                            indexType(t.getName(), el);
                        }
                    }
                }
            }
            if (!roundEnv.processingOver()) {
                return false; //wait until processing is over to generate the files
            }
            writeIndexFiles();
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "[ClassIndexProcessor] Can't write index file: " + e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
            messager.printMessage(Diagnostic.Kind.ERROR, "[ClassIndexProcessor] Internal error: " + e.getMessage());
        }

        return false;
    }

    protected void indexType(String typeName, TypeElement el) {
        Set<String> vals = discovered.get(typeName);
        if (vals == null) {
            vals = new HashSet<>();
            discovered.put(typeName, vals);
        }
        vals.add(getFullName(el));
    }

    protected void writeIndexFiles() throws IOException {
        for (Map.Entry<String, Set<String>> entry : discovered.entrySet()) {
            writeSimpleNameIndexFile(entry.getValue(), prefix + entry.getKey());
        }
    }

    protected void readOldIndexFile(Set<String> entries, String resourceName) throws IOException {
        Reader reader = null;
        try {
            final FileObject resource = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceName);
            reader = resource.openReader(true);
            readOldIndexFile(entries, reader);
        } catch (FileNotFoundException e) {
            /**
             * Ugly hack for Intellij IDEA incremental compilation.
             * The problem is that it throws FileNotFoundException on the files, if they were not created during the
             * current session of compilation.
             */
            final String realPath = e.getMessage();
            if (new File(realPath).exists()) {
                try (Reader fileReader = new FileReader(realPath)) {
                    readOldIndexFile(entries, fileReader);
                }
            }
        } catch (IOException e) {
            // Thrown by Eclipse JDT when not found
        } catch (UnsupportedOperationException e) {
            // Java6 does not support reading old index files
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    protected static void readOldIndexFile(Set<String> entries, Reader reader) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                entries.add(line);
                line = bufferedReader.readLine();
            }
        }
    }

    protected void writeIndexFile(Set<String> entries, String resourceName) throws IOException {
        FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceName);
        try (Writer writer = file.openWriter()) {
            for (String entry : entries) {
                writer.write(entry);
                writer.write("\n");
            }
        }
    }

    protected void writeSimpleNameIndexFile(Set<String> elementList, String resourceName)
            throws IOException {
        readOldIndexFile(elementList, resourceName);
        writeIndexFile(elementList, resourceName);
    }


    protected String getFullName(TypeElement typeElement) {
        switch (typeElement.getNestingKind()) {
            case TOP_LEVEL:
                return typeElement.getQualifiedName().toString();
            case MEMBER:
                final Element enclosingElement = typeElement.getEnclosingElement();
                if (enclosingElement instanceof TypeElement) {
                    final String enclosingName = getFullName(((TypeElement) enclosingElement));
                    if (enclosingName != null) {
                        return enclosingName + '$' + typeElement.getSimpleName().toString();
                    }
                }
                return null;
            case ANONYMOUS:
            case LOCAL:
            default:
                return null;
        }
    }

}
