/**
 * Used to defined any annotation processor which needs to run on the current project's class path.
 * As an FYI - http://stackoverflow.com/a/6974117/400048
 * For info on the annotation processing API see
 * http://docs.oracle.com/javase/6/docs/api/javax/annotation/processing/package-summary.html#package_description
 * http://docs.oracle.com/javase/7/docs/api/javax/annotation/processing/package-summary.html
 * Annotation tutorial
 * http://www.javabeat.net/java-6-0-features-part-2-pluggable-annotation-processing-api/
 * http://www.zdnet.com/writing-and-processing-custom-annotations-part-3-2039362483/
 * http://relation.to/Bloggers/HibernateStaticMetamodelGeneratorAnnotationProcessor (some type safe stuff)
 * http://pilhuhn.blogspot.com/2013/01/when-annotation-processor-does-not.html
 * http://namanmehta.blogspot.com/2010/01/use-codemodel-to-generate-java-source.html
 * Maven optional deps (can affect discoverability of processors)
 * http://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html
 */
package io.higgs.common.annotations;
