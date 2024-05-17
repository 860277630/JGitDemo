package com.example.jgitdemo.JGitOp;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: word-demo
 * @description:  提交
 * @author: wjl
 * @create: 2024-05-16 14:35
 **/
public class AddCommit {
    public static final String BRANCH_PREFIX = "refs/heads/";
    public static void main(String[] args) {
        try (Git git=openRpo("D:\\temp\\myCompare");) {
            // 获取指定分支的引用
            Ref branchRef = git.getRepository().findRef(BRANCH_PREFIX+"master");

            //  添加
            AddCommand add = git.add();
            //  参数 filepattern: 要添加文件/目录的相对路径
            add.addFilepattern("3.txt").call();
            // 提交
            CommitCommand commit1 = git.commit();
            commit1.setMessage("20240516").call();
            //  遍历提交记录

            // 创建RevWalk对象
            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
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
                    System.out.println("Commit id: " +id.getName());
                    System.out.println("-----------------------------------");
                }
            }
        } catch (Exception  e) {
            e.printStackTrace();
        }
    }

    public static Git openRpo(String dir){
        Git git = null;
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(Paths.get(dir, ".git").toFile())
                    .build();
            git = new Git(repository);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return git;
    }
}
