package com.ddd.spec.check.action;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.SneakyThrows;
import net.sourceforge.pmd.*;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.renderers.XMLRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 描述:
 *
 * @author xugangwen
 * @date 2021/10/12 8:52 上午
 */
public class HelloAction extends AnAction {

    @SneakyThrows
    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Module module = e.getData(LangDataKeys.MODULE);

        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        VirtualFile vsiFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        List<VirtualFile> vsiFileList = Lists.newArrayList(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));


        /**依赖**/
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        Module[] dependentModules = moduleRootManager.getDependencies();
        String[] dependentModulesNames = moduleRootManager.getDependencyModuleNames();

        //获取当前类文件的路径
        String classPath = psiFile.getVirtualFile().getPath();
        String title = "Hello World+"+module.getName();

        //显示对话框
//        Messages.showMessageDialog(project, classPath, title, Messages.getInformationIcon());

        PMDConfiguration configuration = new PMDConfiguration();
        String  textSrc = "/Users/bbking/Documents/ideaplugin/ddd-spec-check/src/main/java/test/TErrDto.java";
        configuration.setInputPaths(textSrc);
        configuration.setRuleSets("/Users/bbking/Documents/ideaplugin/ddd-spec-check/src/main/resources/rule/ali-naming.xml");
        //configuration.setReportFormat("json");
//        String rrF = "/Users/bbking/Documents/idea/guide-pmd-master/src/test/java/cn/itedus/demo/test/pmd-report.json";
//        configuration.setReportFile(rrF);
//        ProblemsView.toggleCurrentFileProblems(project,vsiFile);


        RuleSetLoader ruleSetLoader = RuleSetLoader.fromPmdConfig(configuration);
        List<RuleSet> ruleSets = ruleSetLoader.loadFromResources(Arrays.asList(configuration.getRuleSets().split(",")));
        Renderer renderer;
        try {
            List<DataSource> files = determineFiles(textSrc);
            Writer rendererOutput = new StringWriter();
            renderer = createRenderer(rendererOutput);
            renderer.start();
            Report report = PMD.processFiles(configuration, ruleSets, files, Collections.singletonList(renderer));
            List<RuleViolation> violations = report.getViolations();
            StringBuilder msg = new StringBuilder();
            for (RuleViolation ruleViolation:violations){
                System.out.println(ruleViolation.getFilename());
                System.out.println(ruleViolation.getDescription());
                msg.append(ruleViolation.getFilename()).append("/n");
                msg.append(ruleViolation.getDescription()).append("/n");

            }
            Messages.showInfoMessage(msg.toString(),"领域驱动-规范验证-结果");
            renderer.end();
            renderer.flush();
        } catch (IOException ex){
            System.out.print(""+ex.getMessage());
        }finally {
//            ClassLoader auxiliaryClassLoader = configuration.getClassLoader();
//            if (auxiliaryClassLoader instanceof ClasspathClassLoader) {
//                try {
//                    ((ClasspathClassLoader) auxiliaryClassLoader).close();
//                } catch (IOException ex) {
//                    System.out.printf("关闭异常");
//                }
//            }
        }
    }

    private  Renderer createRenderer(Writer writer) {
        XMLRenderer xml = new XMLRenderer("UTF-8");
        xml.setWriter(writer);
        return xml;
    }

    private  List<DataSource> determineFiles(String basePath) throws IOException {
        Path dirPath = FileSystems.getDefault().getPath(basePath);
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.java");
        final List<DataSource> files = new ArrayList<>();
        Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (matcher.matches(path.getFileName())) {
                    System.out.printf("Using %s%n", path);
                    files.add(new FileDataSource(path.toFile()));
                } else {
                    System.out.printf("Ignoring %s%n", path);
                }
                return super.visitFile(path, attrs);
            }
        });
        System.out.printf("Analyzing %d files in %s%n", files.size(), basePath);
        return files;
    }
}
