package com.example.jgitdemo.JGitOp;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class RemoteRes {
    public static void main(String[] args) throws IOException, GitAPIException, URISyntaxException {
        String repoUrl = "http://gitlab.xxx.git";
        String username = "";
        String password = "";
        String branch1 = "master";
        String branch2 = "origin/feat/1.0.0";

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(new File("D:\\temp\\myCompare"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

/*        Repository repository = Git.cloneRepository()
                .setURI(repoUrl)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                .call().getRepository();*/

        ObjectId objectId1 = repository.resolve(branch1);
        ObjectId objectId2 = repository.resolve(branch2);

        try (Git git = new Git(repository); RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit1 = revWalk.parseCommit(objectId1);
            RevCommit commit2 = revWalk.parseCommit(objectId2);

            List<DiffEntry> diffEntries = git.diff()
                    .setOldTree(prepareTreeParser(repository, commit1))
                    .setNewTree(prepareTreeParser(repository, commit2))
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
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit) throws IOException {
        try (RevWalk revWalk = new RevWalk(repository)) {
            RevTree tree = revWalk.parseTree(commit.getTree().getId());
            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader objectReader = repository.newObjectReader()) {
                treeParser.reset(objectReader, tree.getId());
            }
            revWalk.dispose();
            return treeParser;
        }
    }
}


