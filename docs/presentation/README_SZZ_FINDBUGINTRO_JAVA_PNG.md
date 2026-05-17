# Java PNG Renderer: `findBugIntroducingCommits`

This generator creates a presentation-ready PNG for your SZZ flow in
`BasicBugFinder.findBugIntroducingCommits`.

## Source

- `src/main/java/com/research/qmodel/service/findbugs/SzzFindBugIntroPngGenerator.java`

## Output

- `docs/presentation/szz_findBugIntroducingCommits_presentation.png`

## Build and run

```bash
cd /Users/dima/Documents/qmodel
javac -d target/classes src/main/java/com/research/qmodel/service/findbugs/SzzFindBugIntroPngGenerator.java
java -cp target/classes com.research.qmodel.service.findbugs.SzzFindBugIntroPngGenerator docs/presentation/szz_findBugIntroducingCommits_presentation.png
```

## Visual content

- exact method flow of `findBugIntroducingCommits`
- helper behavior from:
  - `getChangedLineNumbers`
  - `traceLineToCommit`
  - `getBlamedCommit`
- stop conditions and presentation message
- 16:9 slide-friendly layout

