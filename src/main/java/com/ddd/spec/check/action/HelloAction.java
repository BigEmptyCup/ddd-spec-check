package com.ddd.spec.check.action;

import cn.hutool.core.map.MapUtil;
import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 描述:
 *
 * @author xugangwen
 * @date 2021/10/12 8:52 上午
 */
public class HelloAction extends AnAction {

    private static final Map<String, List<String>> MODULE_NAME_CHECK_MAP = MapUtil.newHashMap();
    private final String horLine = "-";
    private static final String API = "api";
    private static final String CONTROLLER = "controller";
    private static final String APP = "app";
    private static final String DOMAIN = "domain";
    private static final String COMMON = "common";
    private static final String INFRASTRUCTURE = "infrastructure";
    private static final String BOOT = "boot";

    private static final List<String> EXTERNAL_NAME = Lists.newArrayList("nicetuan_scm_dichi", "bill-platform-manager", "user-info-center");

    static {
        MODULE_NAME_CHECK_MAP.put(API, Lists.newArrayList());
        MODULE_NAME_CHECK_MAP.put(COMMON, Lists.newArrayList());
        MODULE_NAME_CHECK_MAP.put(CONTROLLER, Lists.newArrayList(API, APP));
        MODULE_NAME_CHECK_MAP.put(APP, Lists.newArrayList(DOMAIN));
        MODULE_NAME_CHECK_MAP.put(DOMAIN, Lists.newArrayList(INFRASTRUCTURE, COMMON));
        MODULE_NAME_CHECK_MAP.put(INFRASTRUCTURE, Lists.newArrayList(DOMAIN));
        MODULE_NAME_CHECK_MAP.put(BOOT, Lists.newArrayList(BOOT, API, APP, CONTROLLER, DOMAIN, INFRASTRUCTURE));
    }

    @SneakyThrows
    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Module module = e.getData(LangDataKeys.MODULE);

        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
//        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
//        VirtualFile vsiFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
//        List<VirtualFile> vsiFileList = Lists.newArrayList(e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY));

        ModuleManager manager = ModuleManager.getInstance(project);
        Module[] modules = manager.getModules();
        Arrays.stream(modules).forEach(moduleItem -> {
            String moduleName = moduleItem.getName();
            String shortModuleName = moduleName.substring(moduleName.lastIndexOf(horLine) + 1);
            if (!EXTERNAL_NAME.contains(moduleName) && !MODULE_NAME_CHECK_MAP.containsKey(shortModuleName)) {
                String tip = "模块名称：" + moduleName + "，不符合DDD模块命名规范（" + MODULE_NAME_CHECK_MAP.keySet() + ")";
                Messages.showInfoMessage(tip, "领域驱动-规范验证-结果");
            }
            /**依赖**/
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(moduleItem);
            String[] dependentModulesNames = moduleRootManager.getDependencyModuleNames();
            List<String> configDepModNames = MODULE_NAME_CHECK_MAP.get(shortModuleName);
            AtomicBoolean faultDep = new AtomicBoolean(false);
            Arrays.stream(dependentModulesNames).forEach(dependentModuleName -> {
                String substring = dependentModuleName.substring(dependentModuleName.lastIndexOf(horLine) + 1);
                if (!configDepModNames.contains(substring)) {
                    faultDep.set(true);
                }
            });
            if (faultDep.get()) {
                String tip = "模块名称：" + moduleName + "，不符合DDD模块依赖规范（模块："
                        + moduleName + "--->" + configDepModNames + ",实际依赖了：" + Arrays.asList(dependentModulesNames) + ")";
                Messages.showInfoMessage(tip, "领域驱动-规范验证-结果");
            }
        });

        //获取当前类文件的路径
        String classPath = psiFile.getVirtualFile().getPath();
        String title = "Hello World+" + module.getName();

        //显示对话框
//        Messages.showMessageDialog(project, classPath, title, Messages.getInformationIcon());

        PMDConfiguration configuration = new PMDConfiguration();
        String textSrc = "/Users/bbking/Documents/ideaplugin/ddd-spec-check/src/main/java/test/TErrDto.java";
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
            for (RuleViolation ruleViolation : violations) {
                System.out.println(ruleViolation.getFilename());
                System.out.println(ruleViolation.getDescription());
                msg.append(ruleViolation.getFilename()).append("/n");
                msg.append(ruleViolation.getDescription()).append("/n");

            }
            //Messages.showInfoMessage(msg.toString(), "领域驱动-规范验证-结果");
            renderer.end();
            renderer.flush();
        } catch (IOException ex) {
            System.out.print("" + ex.getMessage());
        } finally {
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

    private Renderer createRenderer(Writer writer) {
        XMLRenderer xml = new XMLRenderer("UTF-8");
        xml.setWriter(writer);
        return xml;
    }

    private List<DataSource> determineFiles(String basePath) throws IOException {
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
