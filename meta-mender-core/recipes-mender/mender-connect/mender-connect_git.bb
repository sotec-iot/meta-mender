require mender-connect.inc

RDEPENDS_${PN} = "glib-2.0 mender-client (>= ${@mender_client_minimum_required_version(d)})"

def mender_client_minimum_required_version(d):
    version = mender_connect_branch_from_preferred_version(d)
    if version.endswith("x"):
        major, minor, *_ = version.split(".")
        if int(major) == 1 and int(minor) <= 2:
            return "2.5"
    return "3.2"

# The revision listed below is not really important, it's just a way to avoid
# network probing during parsing if we are not gonna build the git version
# anyway. If git version is enabled, the AUTOREV will be chosen instead of the
# SHA.
def mender_connect_autorev_if_git_version(d):
    version = d.getVar("PREFERRED_VERSION")
    if version is None or version == "":
        version = d.getVar("PREFERRED_VERSION_%s" % d.getVar('PN'))
    if not d.getVar("EXTERNALSRC") and version is not None and "git" in version:
        return d.getVar("AUTOREV")
    else:
        return "ba20f26b20cffb72ca14ddb3f3e2347186689ace"
SRCREV ?= '${@mender_connect_autorev_if_git_version(d)}'

def mender_connect_branch_from_preferred_version(d):
    import re
    version = d.getVar("PREFERRED_VERSION")
    if version is None or version == "":
        version = d.getVar("PREFERRED_VERSION_%s" % d.getVar('PN'))
    if version is None:
        version = ""
    match = re.match(r"^[0-9]+\.[0-9]+\.", version)
    if match is not None:
        # If the preferred version is some kind of version, use the branch name
        # for that one (1.0.x style).
        return match.group(0) + "x"
    else:
        # Else return master as branch.
        return "master"
MENDER_CONNECT_BRANCH = "${@mender_connect_branch_from_preferred_version(d)}"

def mender_connect_version_from_preferred_version(d, srcpv):
    pref_version = d.getVar("PREFERRED_VERSION")
    if pref_version is not None and pref_version.find("-git") >= 0:
        # If "-git" is in the version, remove it along with any suffix it has,
        # and then readd it with commit SHA.
        return "%s-git%s" % (pref_version[0:pref_version.index("-git")], srcpv)
    elif pref_version is not None and pref_version.find("-build") >= 0:
        # If "-build" is in the version, use the version as is. This means that
        # we can build tags with "-build" in them from this recipe, but not
        # final tags, which will need their own recipe.
        return pref_version
    else:
        # Else return the default "master-git".
        return "master-git%s" % srcpv
PV = "${@mender_connect_version_from_preferred_version(d, '${SRCPV}')}"

SRC_URI = "git://github.com/mendersoftware/mender-connect.git;protocol=https;branch=${MENDER_CONNECT_BRANCH}"

# DO NOT change the checksum here without make sure that ALL licenses (including
# dependencies) are included in the LICENSE variable below. Note that for
# releases, we must check the LIC_FILES_CHKSUM.sha256 file, not the LICENSE
# file.
def mender_connect_license(branch):
    # Only one currently. If the sub licenses change we may introduce more.
    return {
               "license": "Apache-2.0 & BSD-2-Clause & BSD-3-Clause & ISC & MIT",
    }
LIC_FILES_CHKSUM = "file://src/github.com/mendersoftware/mender-connect/LICENSE;md5=fbe9cd162201401ffbb442445efecfdc"
LICENSE = "${@mender_connect_license(d.getVar('MENDER_CONNECT_BRANCH'))['license']}"

# Downprioritize this recipe in version selections.
DEFAULT_PREFERENCE = "-1"
