### Official steps to install ik analyzer plugin
* download pre-build package from here: https://github.com/medcl/elasticsearch-analysis-ik/releases

* create plugin folder cd your-es-root/plugins/ && mkdir ik

* unzip plugin to folder your-es-root/plugins/ik  

restart

### Build a docker image with IK analyzer plugin installed
```jshelllanguage
cd config/

./buildDocker.sh

```
A new image ```elasticsearch-plugins:7.9.3``` is built.



