## Running AutoRefactor (Leafactor)

**Note**: this tutorial was created for *Eclipse Neon*.

1. Install the Plugin Development Environment (PDE) and the resources for Java Development Tools (JDT) development into your Eclipse installation:
    - Help menu > Install New Software...
    - In the opening window: Work with: select: The Eclipse Project Updates - http://download.eclipse.org/eclipse/updates/4.5 (4.5 for Mars, 4.6 for Neon, etc.)
    - Unfold Eclipse Plugin Development Tools, and select Eclipse PDE Plug-in Developer Resources and Eclipse Plug-in Development Environment
    - Unfold Eclipse Java Development Tools, and select Eclipse JDT Plug-in Developer Resources
    - Unfold Eclipse Platform SDK, and select Eclipse Platform SDK
    - Then click the Next >, Next > buttons and then Finish
    - Then restart Eclipse once asked to
2. Install the Tycho Configurator Plugin into your Eclipse installation:
    - Windows menu > Preferences
    - In the opening window: unfold Maven > Discovery, then click the Open Catalog button
    - In the opening window: type "tycho" in the Find field
    - Select "Tycho Configurator", and click the Finish button
3. Clone AutoRefactor from Github (uri: https://github.com/luiscruz/autorefactor) and checkout branch `android-nohacks`.
3. Import the project using the wizard General > *Projects from Folder or Archive*.
4. Run an instance of Eclipse with AutoRefactor:
    - Select project `org.autorefactor.plugin`
    - Run menu > Debug Configurations...
    - then right-click on Eclipse Application, then New
    - type "AutoRefactor" in the Name field, click Apply button, then click Debug button
5. Create a project with your Android app.
    - (this might be tricky since Eclipse is not able to run some Android apps)
    - Make sure you can build your app with Eclipse. (If not, there is a hack to do it, although it is not trivial.)
6. Apply AutoRefactor
    - Right-click in a package or file and then click on the menu *AutoRefactor* > *Automatic Refactoring*
    - In order to check what has changed, I usually use `git status` (assuming you are using git in your project). Alternatively you can run AutoRefactor file by file.

