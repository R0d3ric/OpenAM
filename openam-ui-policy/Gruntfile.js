/* This default Gruntfile only watches for changes to the src/main/resources files and 
 * copies them to the target/openidm_project folder. This makes it easier to develop your project
 * in your source working copy without having to manually rebuild the target after each change.
 * 
 * If you need more sophisticated build processes (such as UI minification, for example) then you 
 * can add that logic to this file with additional grunt plugins (be sure to update package.json)
*/
module.exports = function(grunt) {

    grunt.initConfig({
        buildNumber: process.env.OPENAM_VERSION,
        destination: process.env.OPENAM_HOME,
        forgerockui: process.env.FORGEROCK_UI_SRC,
        replace: {
            html: {
                src: ['src/main/resources/index.html'],
                dest: '<%= destination %>/policyEditor/index.html',
                replacements: [{
                    from: '${version}',
                    to: function () {
                        return '<%= buildNumber %>';
                    }
                }]
            },
            style: {
                src: ['src/main/resources/css/styles.less'],
                dest: '<%= destination %>/policyEditor/css/styles.less',
                replacements: [{
                    from: '${version}',
                    to: function () {
                        return '<%= buildNumber %>';
                    }
                }]
            }
        },
        sync: {
            policyEditor: {
                files: [{
                    cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/js',
                    src     : ['**/*'],
                    dest    : '<%= destination %>/policyEditor'
                },
                {
                    cwd     : '<%= forgerockui %>/forgerock-ui-commons/src/main/resources',
                    src     : ['**/*'],
                    dest    : '<%= destination %>/policyEditor'
                },
                {
                    cwd     : 'src/main/resources',
                    src     : ['**/*'],
                    dest    : '<%= destination %>/policyEditor'
                },
                {
                    cwd     : 'src/main/js',
                    src     : ['**/*'],
                    dest    : '<%= destination %>/policyEditor'
                }]
            }
        },
        watch: {
            policyEditor: {
                files: [
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/js/**',
                    '<%= forgerockui %>/forgerock-ui-commons/src/main/resources/**',
                    'src/main/js/**',
                    'src/main/resources/**'
                ],
                tasks: [ 'sync', 'replace' ]
            }
        }
    });

    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-sync');
    grunt.loadNpmTasks('grunt-text-replace');

    grunt.registerTask('default', ['sync', 'replace', 'watch']);

};
