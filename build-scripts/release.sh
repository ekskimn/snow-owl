#!/bin/bash
##
#  This script can be used to perform a release of the project.
#  Changes in develop will be merged to master. The master and develop versions will be bumped. Branches will be pushed back to git.
##

branchPrefix=$1
echo "branch prefix '${branchPrefix}'"

set -e
echo "Checking out ${branchPrefix}develop"
git checkout "${branchPrefix}develop"
echo "Pulling commits."
git pull
echo "Checking out ${branchPrefix}master."
git checkout "${branchPrefix}master"
echo "Pulling commits."
git pull
echo "Merging ${branchPrefix}develop into ${branchPrefix}master."
git merge "${branchPrefix}develop"

snapVersion=`grep -m1 '<version>' pom.xml | sed 's/.*>\(.[^<]*\).*/\1/'`
newRelease=`echo $snapVersion | sed 's/-SNAPSHOT//'`
newMinor=`echo $((\`echo $newRelease | sed 's/[0-9]*\.[0-9]*\.\([0-9]*\)/\1/'\` + 1))`
newSnap=`echo $newRelease | sed 's/\([0-9]*\)$//' | tr -d '\n';echo "${newMinor}-SNAPSHOT"`

echo "Updating ${branchPrefix}master version. New release version is '${newRelease}'"
mvn -Pdependencies -Dtycho.mode=maven \
 org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion="${newRelease}"

echo "${branchPrefix}master root pom changes."
git diff pom.xml

#read -p "Commit master changes? " -n 1 -r; echo; if [[ ! $REPLY =~ ^[Yy]$ ]]; then echo "Exiting"; exit 1; fi

echo "Commiting ${branchPrefix}master changes."
git commit -a -m "IHTSDO ${branchPrefix}Release ${newRelease}"

git log -n5
#read -p "Push master changes? " -n 1 -r; echo; if [[ ! $REPLY =~ ^[Yy]$ ]]; then echo "Exiting"; exit 1; fi
echo "Pushing ${branchPrefix}master."
# git push


echo "Checking out ${branchPrefix}develop."
git checkout "${branchPrefix}develop"

echo "Merging ${branchPrefix}master into ${branchPrefix}develop."
git merge "${branchPrefix}master"

echo "Updating ${branchPrefix}develop version. New Snapshot version is '${newSnap}'"
mvn -Pdependencies -Dtycho.mode=maven \
 org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion="${newSnap}"

echo "${branchPrefix}develop root pom changes."
git diff pom.xml

# read -p "Commit develop changes? " -n 1 -r; echo; if [[ ! $REPLY =~ ^[Yy]$ ]]; then echo "Exiting"; exit 1; fi

echo "Commiting ${branchPrefix}develop changes."
git commit -a -m "IHTSDO ${branchPrefix}dev version ${newSnap}"

git log -n5
# read -p "Push develop changes? " -n 1 -r; echo; if [[ ! $REPLY =~ ^[Yy]$ ]]; then echo "Exiting"; exit 1; fi
echo "Pushing ${branchPrefix}develop."
# git push
