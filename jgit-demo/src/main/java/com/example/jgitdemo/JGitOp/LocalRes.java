package com.example.jgitdemo.JGitOp;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class LocalRes {
    public static void main(String[] args) throws Exception{
        Git git=openRpo("D:\\temp\\myCompare");
        Repository repository = git.getRepository();
        List<Ref> call = git.branchList().call();     //得到所有分支信息
        for(Ref ref : call)
            System.out.println(ref.getName());

        AbstractTreeIterator newTreeIter = prepareTreeParser(git.getRepository(), git.getRepository().resolve("b033e332747c3e2a23e02acde4f5e81304165bb3").getName());
        AbstractTreeIterator oldTreeIter = prepareTreeParser(git.getRepository(), git.getRepository().resolve("4cc125367a3a76ef9c6af786ce7c24d5e99deac9").getName());
        List<DiffEntry> diffEntries = git.diff()
                .setNewTree(newTreeIter)  //设置源，不设置则默认工作区和历史最新commit版本比较
                .setOldTree(oldTreeIter)
                .call();
        try (DiffFormatter diffFormatter = new DiffFormatter(System.out)) {
            diffFormatter.setRepository(repository);
            diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
            diffFormatter.setDetectRenames(true);

            for (DiffEntry diffEntry : diffEntries) {
                diffFormatter.format(diffEntry);
            }
        }


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


