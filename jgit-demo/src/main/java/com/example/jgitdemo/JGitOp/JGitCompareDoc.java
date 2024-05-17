package com.example.jgitdemo.JGitOp;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: word-demo
 * @description: 用Jgit比较两个文档
 * @author: wjl
 * @create: 2024-05-16 14:53
 **/
public class JGitCompareDoc {

    public static final String driPath = "D:\\temp\\myCompare";

    public static final String COMPARE_PATH1 = "D:\\compare1.txt";
    public static final String COMPARE_PATH2 = "D:\\compare2.txt";
    public static final String COMMIT_FILE_NAME = "commit.txt";

    public static void main(String[] args) throws Exception {
        // 首先验证源文件是否存在 不存在就直接返回
        if (!(new File(COMPARE_PATH1).exists() && new File(COMPARE_PATH2).exists())) {
            System.out.println("源文件不存在，代码中断");
            return;
        }
        // 创建文件夹
        mkDir(driPath);
        // 初始化文件夹
        Git git = Git.init().setDirectory(new File(driPath)).call();
        // 首先把第一个文件拷贝至git仓库中
        copyAndChangeName(COMPARE_PATH1, driPath + "\\" + COMMIT_FILE_NAME);
        // 然后进行添加 提交
        gitAddCommmit(git);
        // 然后把第二个文件拷贝进去
        copyAndChangeName(COMPARE_PATH2, driPath + "\\" + COMMIT_FILE_NAME);
        // 然后进行添加 提交
        gitAddCommmit(git);
        // 然后  获取  最近两次的  差异
        Repository repository = git.getRepository();
        List<Ref> call = git.branchList().call();     //得到所有分支信息
        //  因为只需要  一个  分支 所以  只取第一个
        String branchName = call.get(0).getName();
        List<String> branchHistory = getBranchHistory(branchName, git);
        //  然后获取 两个提交记录的 差异
        getDiffs(git, repository, branchHistory);
        git.close();
        // 最后删除这个文件夹
        boolean delete = new File(driPath).delete();
        System.out.println(delete ? "删除成功" : "删除失败，请考虑定时任务");
    }


    private static void getDiffs(Git git, Repository repository, List<String> historys) throws Exception {
        AbstractTreeIterator newTreeIter = prepareTreeParser(git.getRepository(), git.getRepository().resolve(historys.get(0)).getName());
        AbstractTreeIterator oldTreeIter = prepareTreeParser(git.getRepository(), git.getRepository().resolve(historys.get(1)).getName());
        List<DiffEntry> diffEntries = git.diff()
                .setNewTree(newTreeIter)  //设置源，不设置则默认工作区和历史最新commit版本比较
                .setOldTree(oldTreeIter)
                .call();
/*        try (DiffFormatter diffFormatter = new DiffFormatter(System.out)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);
            for (DiffEntry diffEntry : diffEntries) {
                diffFormatter.format(diffEntry);
            }
        }*/
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for (DiffEntry entry : diffEntries) {
            DiffFormatter formatter = new DiffFormatter(byteStream) ;
            formatter.setRepository(repository);
            formatter.format(entry);
        }
        String diffContent = byteStream.toString();
        System.out.println(diffContent);
    }

    public static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader()) {
                treeParser.reset(reader, tree.getId());
            }
            walk.dispose();
            return treeParser;
        }
    }


    //  获取分支上所有的提交记录
    private static List<String> getBranchHistory(String branchName, Git git) throws Exception {
        List<String> result = new ArrayList<>();
        // 然后获取  这个分支上的所有  提交记录
        Ref branchRef = git.getRepository().findRef(branchName);
        //  遍历提交记录
        RevWalk revWalk = new RevWalk(git.getRepository());
        // 从分支的引用获取提交对象
        RevCommit commit = revWalk.parseCommit(branchRef.getObjectId());
        List<RevCommit> commits = new ArrayList<>();

        // 遍历分支上的所有提交记录
        while (commit != null) {
            commits.add(commit);
            commit = commit.getParentCount() > 0 ? revWalk.parseCommit(commit.getParent(0).getId()) : null;
        }

        // 打印提交记录
        for (RevCommit revCommit : commits) {
            System.out.println("Author: " + revCommit.getAuthorIdent().getName());
            System.out.println("Commit Time: " + revCommit.getAuthorIdent().getWhen());
            System.out.println("Commit Message: " + revCommit.getFullMessage());
            ObjectId id = revCommit.getId();
            System.out.println("Commit id: " + id.getName());
            System.out.println("-----------------------------------");
            result.add(id.getName());
        }
        return result;
    }

    //  添加提交
    private static void gitAddCommmit(Git git) throws Exception {
        //  添加
        AddCommand add = git.add();
        //  参数 filepattern: 要添加文件/目录的相对路径
        add.addFilepattern(COMMIT_FILE_NAME).call();
        // 提交
        CommitCommand commit1 = git.commit();
        commit1.setMessage(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).call();
    }

    //  将文件拷贝至目标文件夹  并且改名
    private static void copyAndChangeName(String originFilePath, String targetFilePath) {
        File originFile = new File(originFilePath);
        File targetFile = new File(targetFilePath);
        // 首先将原先的 文件删除
        if (targetFile.exists()) {
            targetFile.delete();
        }
        originFile.renameTo(targetFile);
    }

    // 创建文件夹
    private static void mkDir(String driPath) {
        File folder = new File(driPath);
        //  如果存在 就先删掉
        if (folder.exists()) {
            folder.delete();
        }
        //  然后进行创建
        folder.mkdir();
    }
}
